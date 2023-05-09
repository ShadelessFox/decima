package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(@Type(type = RTTIReference.class))
public class ReferenceValueHandler implements ValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            if (value instanceof RTTIReference.External ref) {
                component.append("path = %s, ".formatted(ref.path()), TextAttributes.REGULAR_ATTRIBUTES);
                component.append("uuid = ", TextAttributes.REGULAR_ATTRIBUTES);
                GGUUIDValueHandler.INSTANCE.getDecorator(ref.uuid().type()).decorate(ref.uuid(), component);
                component.append(", kind = " + ref.kind(), TextAttributes.REGULAR_ATTRIBUTES);
            } else if (value instanceof RTTIReference.Internal ref) {
                component.append("uuid = ", TextAttributes.REGULAR_ATTRIBUTES);
                GGUUIDValueHandler.INSTANCE.getDecorator(ref.uuid().type()).decorate(ref.uuid(), component);
                component.append(", kind = " + ref.kind(), TextAttributes.REGULAR_ATTRIBUTES);
            } else {
                component.append("none", TextAttributes.REGULAR_ATTRIBUTES);
            }
        };
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.referenceIcon");
    }

    @Nullable
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        if (value instanceof RTTIReference.External ref) {
            return ref.path();
        } else {
            return null;
        }
    }
}
