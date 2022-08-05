package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.controls.ColoredComponent;
import com.shade.decima.ui.controls.TextAttributes;
import com.shade.decima.ui.handler.ValueHandler;

public class ReferenceValueHandler implements ValueHandler {
    public static final ReferenceValueHandler INSTANCE = new ReferenceValueHandler();

    private ReferenceValueHandler() {
    }

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        final RTTIReference ref = (RTTIReference) value;
        final String path = ref.path();
        final RTTIObject uuid = ref.uuid();

        if (path != null && uuid != null) {
            component.append("path = %s, ".formatted(path), TextAttributes.REGULAR_ATTRIBUTES);
            component.append("uuid = ", TextAttributes.REGULAR_ATTRIBUTES);
            GGUUIDValueHandler.INSTANCE.appendInlineValue(uuid.getType(), uuid, component);
        } else if (uuid != null) {
            component.append("uuid = ", TextAttributes.REGULAR_ATTRIBUTES);
            GGUUIDValueHandler.INSTANCE.appendInlineValue(uuid.getType(), uuid, component);
        } else {
            component.append("none", TextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }
}
