package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

public class UnsignedNumberValueHandler implements ValueHandler {
    public static final UnsignedNumberValueHandler INSTANCE = new UnsignedNumberValueHandler();

    private UnsignedNumberValueHandler() {
    }

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        final String formatted;

        if (value instanceof Byte v) {
            formatted = String.valueOf(Byte.toUnsignedInt(v));
        } else if (value instanceof Short v) {
            formatted = String.valueOf(Short.toUnsignedInt(v));
        } else if (value instanceof Integer v) {
            formatted = Integer.toUnsignedString(v);
        } else if (value instanceof Long v) {
            formatted = Long.toUnsignedString(v);
        } else {
            formatted = String.valueOf(value);
        }

        component.append(formatted, TextAttributes.BLUE_ATTRIBUTES);
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }
}
