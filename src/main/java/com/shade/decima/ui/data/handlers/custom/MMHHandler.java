package com.shade.decima.ui.data.handlers.custom;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.hash.CRC32C;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.decima.ui.data.handlers.StringValueHandler;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

@ValueHandlerRegistration(value = @Type(type = String.class), id = "weirdHash", name = "Binding Hash", order = 100)
public class MMHHandler extends StringValueHandler {

    private String sHash(@NotNull String value) {
        final int crc = CRC32C.calculate(value.getBytes(StandardCharsets.US_ASCII));
        return "0x" + IOUtils.toHexDigits(crc, ByteOrder.LITTLE_ENDIAN).toLowerCase();

    }


    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        if (value instanceof String) {
            return sHash((String) value);
        }
        return "INVALID";
    }

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            component.append(sHash((String) value), TextAttributes.REGULAR_ATTRIBUTES);
        };
    }
}