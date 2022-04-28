package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.handler.ValueHandler;

public class UnsignedNumberValueHandler implements ValueHandler {
    public static final UnsignedNumberValueHandler INSTANCE = new UnsignedNumberValueHandler();

    private UnsignedNumberValueHandler() {
    }

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
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

        return "<font color=#1750eb>%s</font>".formatted(formatted);
    }
}
