package com.shade.decima.ui.data.handlers.custom;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeString;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.decima.ui.data.handlers.StringValueHandler;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

@ValueHandlerRegistration(value = @Type(type = RTTITypeString.class), id = "mmhHash", name = "MMH Hash", order = 100)
public class MMHHandler extends StringValueHandler {
    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        if (value instanceof String) {
            final long[] longs = MurmurHash3.mmh3(((String) value).getBytes(StandardCharsets.UTF_8));
            return "%x08".formatted(longs[0]);
        }
        return "INVALID";
    }

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final String data = ((String) value);
            final long[] longs = MurmurHash3.mmh3((data).getBytes(StandardCharsets.UTF_8));

            component.append(IOUtils.toHexDigits(longs[0], ByteOrder.LITTLE_ENDIAN), TextAttributes.REGULAR_ATTRIBUTES);
            component.append(" | ", TextAttributes.REGULAR_ATTRIBUTES);
            component.append(IOUtils.toHexDigits(longs[1], ByteOrder.LITTLE_ENDIAN), TextAttributes.REGULAR_ATTRIBUTES);
        };
    }
}