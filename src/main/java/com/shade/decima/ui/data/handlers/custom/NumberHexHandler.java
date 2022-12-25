package com.shade.decima.ui.data.handlers.custom;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.handlers.NumberValueHandler;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.util.NotNull;

@ValueHandlerRegistration(value = @Type(type = Number.class), id = "numberHex", name = "Hexadecimal", order = 100)
public class NumberHexHandler extends NumberValueHandler {
    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        if (value instanceof Byte v) {
            return "%#02x".formatted(v);
        } else if (value instanceof Short v) {
            return "%#04x".formatted(v);
        } else if (value instanceof Integer v) {
            return "%#08x".formatted(v);
        } else if (value instanceof Long v) {
            return "%#016x".formatted(v);
        } else {
            return value.toString();
        }
    }
}
