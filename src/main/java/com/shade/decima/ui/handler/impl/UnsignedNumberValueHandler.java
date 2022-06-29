package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.controls.ColoredComponent;
import com.shade.decima.ui.controls.TextAttributes;
import com.shade.decima.ui.handler.ValueHandler;

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
