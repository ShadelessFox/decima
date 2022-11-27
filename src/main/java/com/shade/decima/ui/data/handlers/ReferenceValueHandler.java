package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.RTTITypeReference;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(@Type(type = RTTITypeReference.class))
public class ReferenceValueHandler implements ValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIReference ref = (RTTIReference) value;
            final String path = ref.path();
            final RTTIObject uuid = ref.uuid();

            if (path != null && uuid != null) {
                component.append("path = %s, ".formatted(path), TextAttributes.REGULAR_ATTRIBUTES);
                component.append("uuid = ", TextAttributes.REGULAR_ATTRIBUTES);
                GGUUIDValueHandler.INSTANCE.getDecorator(uuid.type()).decorate(uuid, component);
            } else if (uuid != null) {
                component.append("uuid = ", TextAttributes.REGULAR_ATTRIBUTES);
                GGUUIDValueHandler.INSTANCE.getDecorator(uuid.type()).decorate(uuid, component);
            } else {
                component.append("none", TextAttributes.REGULAR_ATTRIBUTES);
            }
        };
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("CoreEditor.referenceIcon");
    }

    @Nullable
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        return ((RTTIReference) value).path();
    }
}
