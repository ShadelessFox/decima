package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteOrder;

@ValueHandlerRegistration(id = "numberHex", name = "Hexadecimal", order = 100, value = {
    @Selector(type = @Type(type = Number.class))
})
public class NumberHexHandler extends NumberValueHandler {
    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        if (value instanceof Byte v) {
            return "0x" + IOUtils.toHexDigits(v);
        } else if (value instanceof Short v) {
            return "0x" + IOUtils.toHexDigits(v, ByteOrder.BIG_ENDIAN);
        } else if (value instanceof Integer v) {
            return "0x" + IOUtils.toHexDigits(v, ByteOrder.BIG_ENDIAN);
        } else if (value instanceof Long v) {
            return "0x" + IOUtils.toHexDigits(v, ByteOrder.BIG_ENDIAN);
        } else {
            return value.toString();
        }
    }
}
