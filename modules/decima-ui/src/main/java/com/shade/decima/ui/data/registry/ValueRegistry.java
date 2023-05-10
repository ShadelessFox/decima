package com.shade.decima.ui.data.registry;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.handlers.DefaultValueHandler;
import com.shade.platform.model.ExtensionRegistry;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.model.util.IOUtils;
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
        this.managers = ExtensionRegistry.getExtensions(ValueManager.class, ValueManagerRegistration.class);
        this.viewers = ExtensionRegistry.getExtensions(ValueViewer.class, ValueViewerRegistration.class);
        this.handlers = ExtensionRegistry.getExtensions(ValueHandler.class, ValueHandlerRegistration.class).stream()
            .sorted(Comparator.comparingInt(handler -> handler.metadata().order()))
            .toList();
    }

    @NotNull
    public static ValueRegistry getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> ValueManager<T> findManager(@NotNull Object value, @NotNull RTTIType<?> rttiType, @NotNull GameType gameType) {
        for (var manager : managers) {
            if (matches(manager.metadata().value(), value, rttiType, gameType)) {
                return (ValueManager<T>) manager.get();
            }
        }

        return null;
    }

    @Nullable
    public ValueViewer findViewer(@NotNull Object value, @NotNull RTTIType<?> rttiType, @NotNull GameType gameType) {
        for (var viewer : viewers) {
            if (matches(viewer.metadata().value(), value, rttiType, gameType)) {
                return viewer.get();
            }
        }

        return null;
    }

    @NotNull
    public ValueHandler findHandler(@NotNull Object value, @NotNull RTTIType<?> rttiType, @NotNull GameType gameType) {
        for (var handler : handlers) {
            if (matches(handler.metadata().value(), value, rttiType, gameType)) {
                return handler.get();
            }
        }

        return DefaultValueHandler.INSTANCE;
    }

    @NotNull
    public List<LazyWithMetadata<ValueHandler, ValueHandlerRegistration>> findHandlers(@NotNull Object value, @NotNull RTTIType<?> rttiType, @NotNull GameType gameType) {
        return handlers.stream()
            .filter(handler -> matches(handler.metadata().value(), value, rttiType, gameType))
            .toList();
    }

    private static boolean matches(@NotNull Type[] types, @NotNull Object value, @NotNull RTTIType<?> rttiType, @NotNull GameType gameType) {
        for (Type type : types) {
            if (matches(type, value, rttiType, gameType)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matches(@NotNull Type type, @NotNull Object value, @NotNull RTTIType<?> rttiType, @NotNull GameType gameType) {
        if (type.name().isEmpty() && type.type() == Void.class) {
            throw new IllegalArgumentException("The Type must either specify Type#name or Type#type");
        }

        if (type.game().length > 0 && IOUtils.indexOf(type.game(), gameType) < 0) {
            return false;
        }

        if (type.type() != Void.class) {
            if (type.type().isAssignableFrom(rttiType.getInstanceType())) {
                return true;
            } else if (type.type().isInstance(value)) {
                return true;
            } else if (value instanceof RTTIObject object && type.type().isInstance(object.data())) {
                return true;
            }
        }

        return value instanceof RTTIObject object && object.type().isInstanceOf(type.name());
    }
}
