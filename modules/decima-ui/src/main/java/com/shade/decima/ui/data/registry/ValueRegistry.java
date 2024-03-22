package com.shade.decima.ui.data.registry;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.handlers.DefaultValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Field;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.model.ExtensionRegistry;
import com.shade.platform.model.LazyWithMetadata;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.Comparator;
import java.util.List;

public class ValueRegistry {
    private static final ValueRegistry INSTANCE = new ValueRegistry();

    private static final Type DEFAULT_SELECTOR_TYPE;
    private static final Field DEFAULT_SELECTOR_FIELD;

    static {
        try {
            DEFAULT_SELECTOR_TYPE = (Type) Selector.class.getDeclaredMethod("type").getDefaultValue();
            DEFAULT_SELECTOR_FIELD = (Field) Selector.class.getDeclaredMethod("field").getDefaultValue();
        } catch (Throwable ignored) {
            throw new AssertionError("should not happen");
        }
    }

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
    public <T> ValueManager<T> findManager(@NotNull ValueController<?> controller) {
        for (var manager : managers) {
            if (matches(manager.metadata().value(), controller)) {
                return (ValueManager<T>) manager.get();
            }
        }

        return null;
    }

    @Nullable
    public ValueViewer findViewer(@NotNull ValueController<?> controller) {
        for (var viewer : viewers) {
            if (matches(viewer.metadata().value(), controller)) {
                return viewer.get();
            }
        }

        return null;
    }

    @NotNull
    public ValueHandler findHandler(@NotNull ValueController<?> controller) {
        for (var handler : handlers) {
            if (matches(handler.metadata().value(), controller)) {
                return handler.get();
            }
        }

        return DefaultValueHandler.INSTANCE;
    }

    @NotNull
    public List<LazyWithMetadata<ValueHandler, ValueHandlerRegistration>> findHandlers(@NotNull ValueController<?> controller) {
        return handlers.stream()
            .filter(handler -> matches(handler.metadata().value(), controller))
            .toList();
    }

    private static boolean matches(@NotNull Selector[] selectors, @NotNull ValueController<?> controller) {
        for (Selector selector : selectors) {
            if (matches(selector, controller)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matches(@NotNull Selector selector, @NotNull ValueController<?> controller) {
        if (selector.game().length > 0 && !IOUtils.contains(selector.game(), controller.getProject().getContainer().getType())) {
            return false;
        }

        if (!DEFAULT_SELECTOR_TYPE.equals(selector.type())) {
            return matches(selector.type(), controller);
        }

        if (!DEFAULT_SELECTOR_FIELD.equals(selector.field())) {
            return matches(selector.field(), controller);
        }

        throw new IllegalArgumentException("The @Selector must either specify a field or a type");
    }

    private static boolean matches(@NotNull Type value, @NotNull ValueController<?> controller) {
        if (value.type() != Void.class) {
            if (value.type().isAssignableFrom(controller.getValueType().getInstanceType())) {
                return true;
            } else if (value.type().isInstance(controller.getValue())) {
                return true;
            } else if (controller.getValue() instanceof RTTIObject object && value.type().isInstance(object.data())) {
                return true;
            }
        }

        return controller.getValue() instanceof RTTIObject object && object.type().isInstanceOf(value.name())
            || value.name().equals(controller.getValueType().getFullTypeName());
    }

    private static boolean matches(@NotNull Field value, @NotNull ValueController<?> controller) {
        final RTTIPath path = controller.getValuePath();

        if (path != null && path.last() instanceof RTTIPathElement.Field f) {
            final RTTIClass.Field<Object> field = f.get();
            return value.field().equals(field.getName())
                && value.type().equals(field.getParent().getFullTypeName());

        }

        return false;
    }
}
