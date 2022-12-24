package com.shade.decima.ui.data.registry;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.handlers.DefaultValueHandler;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.Comparator;
import java.util.List;

public class ValueRegistry {
    private static final ValueRegistry INSTANCE = new ValueRegistry();

    @SuppressWarnings("rawtypes")
    private final List<LazyWithMetadata<ValueManager, ValueManagerRegistration>> managers;
    private final List<LazyWithMetadata<ValueViewer, ValueViewerRegistration>> viewers;
    private final List<LazyWithMetadata<ValueHandler, ValueHandlerRegistration>> handlers;

    private ValueRegistry() {
        this.managers = ReflectionUtils.findAnnotatedTypes(ValueManager.class, ValueManagerRegistration.class);
        this.viewers = ReflectionUtils.findAnnotatedTypes(ValueViewer.class, ValueViewerRegistration.class);
        this.handlers = ReflectionUtils.findAnnotatedTypes(ValueHandler.class, ValueHandlerRegistration.class);
        this.handlers.sort(Comparator.comparingInt(handler -> handler.metadata().order()));
    }

    @NotNull
    public static ValueRegistry getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> ValueManager<T> findManager(@NotNull Object value) {
        for (var manager : managers) {
            if (matches(manager.metadata().value(), value, null)) {
                return (ValueManager<T>) manager.get();
            }
        }

        return null;
    }

    @Nullable
    public ValueViewer findViewer(@NotNull Object value, @Nullable GameType gameType) {
        for (var viewer : viewers) {
            if (matches(viewer.metadata().value(), value, gameType)) {
                return viewer.get();
            }
        }

        return null;
    }

    @NotNull
    public ValueHandler findHandler(@NotNull Object value, @Nullable GameType game) {
        for (var handler : handlers) {
            if (matches(handler.metadata().value(), value, game)) {
                return handler.get();
            }
        }

        return DefaultValueHandler.INSTANCE;
    }

    @NotNull
    public List<LazyWithMetadata<ValueHandler, ValueHandlerRegistration>> findHandlers(@NotNull Object value, @Nullable GameType game) {
        return handlers.stream()
            .filter(handler -> matches(handler.metadata().value(), value, game))
            .toList();
    }

    private static boolean matches(@NotNull Type[] types, @NotNull Object value, @Nullable GameType game) {
        for (Type type : types) {
            if (matches(type, value, game)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matches(@NotNull Type type, @NotNull Object value, @Nullable GameType game) {
        if (type.name().isEmpty() && type.type() == Void.class) {
            throw new IllegalArgumentException("The Type must either specify Type#name or Type#type");
        }

        if (game != null && type.game().length > 0 && IOUtils.indexOf(type.game(), game) < 0) {
            return false;
        }

        if (type.type() != Void.class) {
            return type.type().isInstance(value);
        }

        return value instanceof RTTIObject object && object.type().isInstanceOf(type.name());
    }
}
