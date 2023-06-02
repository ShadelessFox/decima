package com.shade.platform.model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceManager implements Disposable {
    private static final Logger log = LoggerFactory.getLogger(ServiceManager.class);
    private static final Gson gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(Path.class, new PathAdapter())
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private final Path path;
    private final Map<Class<?>, ServiceInfo> services;
    private final Map<String, JsonElement> states;

    public ServiceManager(@NotNull Path path) {
        this.path = path;
        this.services = new LinkedHashMap<>();
        this.states = new LinkedHashMap<>();

        for (LazyWithMetadata<Object, Service> service : ExtensionRegistry.getExtensions(Object.class, Service.class)) {
            services.put(service.metadata().value(), new ServiceInfo(service));
        }

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                states.putAll(gson.fromJson(reader, new TypeToken<Map<String, JsonElement>>() {}.getType()));
            } catch (IOException e) {
                log.error("Error loading state", e);
            }
        }
    }

    public <T> T getService(@NotNull Class<? extends T> cls) {
        final ServiceInfo info = services.get(cls);

        if (info != null) {
            return cls.cast(info.get());
        } else {
            return null;
        }
    }

    public void persist() throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());
        }

        try (JsonWriter writer = gson.newJsonWriter(Files.newBufferedWriter(path))) {
            writer.setLenient(false);
            writer.setIndent("\t");

            persist(writer);
        }
    }

    private void persist(@NotNull JsonWriter writer) throws IOException {
        writer.beginObject();

        for (ServiceInfo service : services.values()) {
            saveState(writer, service);
        }

        writer.endObject();
    }

    private void saveState(@NotNull JsonWriter writer, @NotNull ServiceInfo service) throws IOException {
        final Class<?> cls = service.object.type();
        final Persistent persistent = cls.getDeclaredAnnotation(Persistent.class);

        if (persistent == null || !PersistableComponent.class.isAssignableFrom(cls)) {
            return;
        }

        JsonElement state = null;

        if (service.object.isLoaded()) {
            final PersistableComponent<?> component = (PersistableComponent<?>) service.get();
            final Type type = getStateType(component);

            if (type == null) {
                return;
            }

            try {
                state = gson.toJsonTree(component.getState(), type);
            } catch (Exception e) {
                log.error("Error while saving state of service " + component.getClass(), e);
            }
        } else {
            state = states.get(persistent.value());
        }

        if (state == null) {
            state = JsonNull.INSTANCE;
        }

        writer.name(persistent.value());

        if (state.isJsonNull()) {
            writer.setSerializeNulls(true);
            writer.nullValue();
            writer.setSerializeNulls(false);
        } else {
            gson.toJson(state, writer);
        }
    }

    @Override
    public void dispose() {
        try {
            persist();
        } catch (IOException e) {
            log.error("Error while saving state of services", e);
        }
    }

    private class ServiceInfo {
        private final LazyWithMetadata<?, Service> object;
        private volatile boolean initializing;

        public ServiceInfo(@NotNull LazyWithMetadata<?, Service> instance) {
            this.object = instance;
        }

        @NotNull
        public Object get() {
            if (object.isLoaded()) {
                return object.get();
            }

            synchronized (object) {
                if (object.isLoaded()) {
                    return object.get();
                }

                if (initializing) {
                    throw new IllegalStateException("Cyclic service initialization: " + object.type());
                }

                final Object service;

                try {
                    initializing = true;
                    log.debug("Loading service {} ({})", object.metadata().value().getSimpleName(), object.type().getSimpleName());
                    service = object.get();
                } finally {
                    initializing = false;
                }

                if (service instanceof PersistableComponent<?> component) {
                    loadState(component);
                }

                return service;
            }
        }
    }

    private void loadState(@NotNull PersistableComponent<?> component) {
        final Persistent persistent = getPersistent(component.getClass());
        final Type type = getStateType(component);

        if (persistent == null || type == null) {
            return;
        }

        final JsonElement state = states.get(persistent.value());

        try {
            if (state == null) {
                component.noStateLoaded();
            } else if (!state.isJsonNull()) {
                component.loadState(gson.fromJson(state, type));
            }
        } catch (Throwable e) {
            log.error("Error while loading state of service " + component.getClass(), e);
        }
    }

    @Nullable
    private static Persistent getPersistent(@NotNull Class<?> cls) {
        final Persistent persistent = cls.getDeclaredAnnotation(Persistent.class);

        if (persistent == null) {
            log.warn("The service " + cls
                + " implements " + PersistableComponent.class.getSimpleName()
                + " but isn't annotated with " + Persistent.class.getSimpleName());
        }

        return persistent;
    }

    @Nullable
    private static Type getStateType(@NotNull PersistableComponent<?> component) {
        for (Type iface : component.getClass().getGenericInterfaces()) {
            if (iface instanceof ParameterizedType type && type.getRawType() == PersistableComponent.class) {
                return type.getActualTypeArguments()[0];
            }
        }

        log.warn("The service " + component.getClass()
            + " didn't specify its state type by"
            + " parameterizing " + PersistableComponent.class.getSimpleName());

        return null;
    }

    private static class PathAdapter implements JsonSerializer<Path>, JsonDeserializer<Path> {
        @Override
        public JsonElement serialize(Path src, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Path deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            return Path.of(json.getAsString());
        }
    }
}
