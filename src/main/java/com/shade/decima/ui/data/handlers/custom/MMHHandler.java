package com.shade.decima.ui.data.handlers.custom;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.decima.model.rtti.types.RTTITypeString;
import com.shade.decima.ui.data.handlers.NumberValueHandler;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.util.NotNull;

import java.nio.charset.StandardCharsets;

@ValueHandlerRegistration(value = @Type(type = RTTITypeString.class), id = "mmhHash", name = "MMH Hash", order = 100)
public class MMHHandler extends NumberValueHandler {
    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        if(value instanceof String){
            final long[] longs = MurmurHash3.mmh3(((String) value).getBytes(StandardCharsets.UTF_8));
            return "%x08".formatted(longs[0]);
        }
        return "INVALID";
    }
}