package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;

@ValueHandlerRegistration(id = "numberFloat", name = "Float", order = 500, value = {
    @Selector(type = @Type(type = short.class)),
    @Selector(type = @Type(type = int.class)),
    @Selector(type = @Type(type = long.class))
})
public class NumberFloatHandler extends NumberValueHandler {
    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        if (value instanceof Short v) {
            return String.valueOf(MathUtils.halfToFloat(v));
        } else if (value instanceof Integer v) {
            return String.valueOf(Float.intBitsToFloat(v));
        } else if (value instanceof Long v) {
            return String.valueOf(Double.longBitsToDouble(v));
        } else {
            return value.toString();
        }
    }
}
