package com.shade.platform.model.persistence;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.shade.platform.model.ExtensionRegistry;
import com.shade.platform.model.Lazy;
import com.shade.platform.model.Service;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(PersistenceManager.class);
    private static final Gson gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(Path.class, new PathAdapter())
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private final Map<String, Lazy<PersistableComponent<?>>> components;
    private final Path path;

    @SuppressWarnings("unchecked")
    public PersistenceManager(@NotNull Path path) {
        this.components = ExtensionRegistry
            .getExtensions(PersistableComponent.class, Service.class).stream()
            .collect(Collectors.toMap(
                c -> c.type().getDeclaredAnnotation(Persistent.class).value(),
                c -> (Lazy<PersistableComponent<?>>) (Object) c
            ));
        this.path = path;
    }

    public void load() throws IOException {
        final Set<String> readComponents = new HashSet<>();

        if (Files.exists(path)) {
            try (JsonReader reader = gson.newJsonReader(Files.newBufferedReader(path))) {
                load(reader, readComponents);
            }
        }

        for (var entry : components.entrySet()) {
            if (!readComponents.contains(entry.getKey())) {
                entry.getValue().get().noStateLoaded();
            }
        }
    }

    public void persist() throws IOException {
        try (JsonWriter writer = gson.newJsonWriter(Files.newBufferedWriter(path))) {
            writer.setLenient(false);
            writer.setIndent("\t");

            persist(writer);
        }
    }

    private void load(@NotNull JsonReader reader, @NotNull Set<String> loadedComponents) throws IOException {
        reader.beginObject();

        while (reader.hasNext()) {
            final var name = reader.nextName();
            final var component = components.get(name);

            if (component == null) {
                log.warn("Unknown component: '{}'", name);
                reader.skipValue();
                continue;
            }

            final var instance = component.get();
            final var type = getStateType(instance);

            if (type == null) {
                log.warn("Can't determine type of the component's state: {}", component);
                reader.skipValue();
                continue;
            }

            instance.loadState(gson.fromJson(reader, type));
            loadedComponents.add(name);
        }

        reader.endObject();
    }

    private void persist(@NotNull JsonWriter writer) throws IOException {
        writer.beginObject();

        for (var component : components.entrySet()) {
            final var instance = component.getValue().get();
            final var type = getStateType(instance);

            if (type == null) {
                log.warn("Can't determine type of the component's state: {}", component);
                continue;
            }

            writer.name(component.getKey());
            gson.toJson(instance.getState(), type, writer);
        }

        writer.endObject();
    }

    @Nullable
    private static Type getStateType(@NotNull PersistableComponent<?> component) {
        for (Type iface : component.getClass().getGenericInterfaces()) {
            if (iface instanceof ParameterizedType type && type.getRawType() == PersistableComponent.class) {
                return type.getActualTypeArguments()[0];
            }
        }

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
