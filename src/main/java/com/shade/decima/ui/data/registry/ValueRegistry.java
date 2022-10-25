package com.shade.decima.ui.data.registry;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeClass;
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
    public <T> ValueManager<T> findManager(@NotNull RTTIType<T> rttiType) {
        for (var manager : managers) {
            if (matches(manager.metadata().value(), rttiType, null)) {
                return (ValueManager<T>) manager.get();
            }
        }

        return null;
    }

    @Nullable
    public <T> ValueViewer findViewer(@NotNull RTTIType<T> rttiType, @NotNull GameType gameType) {
        for (var viewer : viewers) {
            if (matches(viewer.metadata().value(), rttiType, gameType)) {
                return viewer.get();
            }
        }

        return null;
    }

    @NotNull
    public <T> ValueHandler findHandler(@NotNull RTTIType<T> rttiType, @NotNull GameType gameType) {
        for (var handler : handlers) {
            if (matches(handler.metadata().value(), rttiType, gameType)) {
                return handler.get();
            }
        }

        return DefaultValueHandler.INSTANCE;
    }

    private static boolean matches(@NotNull Type[] types, @NotNull RTTIType<?> rttiType, @Nullable GameType gameType) {
        for (Type type : types) {
            if (matches(type, rttiType, gameType)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matches(@NotNull Type type, @NotNull RTTIType<?> rttiType, @Nullable GameType gameType) {
        if (type.name().isEmpty() && type.type() == Void.class) {
            throw new IllegalArgumentException("The Type must either specify Type#name or Type#type");
        }

        if (gameType != null && type.game().length > 0 && IOUtils.indexOf(type.game(), gameType) < 0) {
            return false;
        }

        if (type.type() != Void.class) {
            return type.type().isAssignableFrom(rttiType.getClass());
        }

        return rttiType.getTypeName().equals(type.name())
            || rttiType instanceof RTTITypeClass cls && cls.isInstanceOf(type.name());
    }
}
