package com.shade.decima.ui.data.handlers.custom;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.ui.data.handlers.NumberValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.util.NotNull;

@ValueHandlerRegistration(value = @Type(type = Number.class), id = "numberBin", name = "Binary", order = 200)
public class NumberBinHandler extends NumberValueHandler {
    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        if (value instanceof Byte v) {
            return "0b%8s".formatted(Integer.toBinaryString(v & 0xff)).replace(' ', '0');
        } else if (value instanceof Short v) {
            return "0b%16s".formatted(Integer.toBinaryString(v & 0xffff)).replace(' ', '0');
        } else if (value instanceof Integer v) {
            return "0b%32s".formatted(Integer.toBinaryString(v)).replace(' ', '0');
        } else if (value instanceof Long v) {
            return "0b%64s".formatted(Long.toBinaryString(v)).replace(' ', '0');
        } else {
            return value.toString();
        }
    }
}
