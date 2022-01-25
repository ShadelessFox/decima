package com.shade.decima.ui.handlers.impl;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.ui.handlers.ValueHandler;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

public class GGUUIDValueHandler implements ValueHandler {
    public static final GGUUIDValueHandler INSTANCE = new GGUUIDValueHandler();

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        final RTTIObject object = (RTTIObject) value;
        final StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < 16; i++) {
            buffer.append("%02x".formatted(object.<Byte>get("Data" + i)));

            if (i == 4 || i == 6 || i == 8 || i == 10) {
                buffer.append('-');
            }
        }

        return buffer.toString();
    }
}
