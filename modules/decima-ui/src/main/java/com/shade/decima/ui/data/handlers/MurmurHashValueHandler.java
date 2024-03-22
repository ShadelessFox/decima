package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "hash", name = "Hash", value = {
    @Selector(type = @Type(name = "MurmurHashValue"))
})
public class MurmurHashValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(getText(type, value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        final RTTIObject o = (RTTIObject) value;

        return "%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x".formatted(
            o.i8("Data0"), o.i8("Data1"), o.i8("Data2"), o.i8("Data3"),
            o.i8("Data4"), o.i8("Data5"), o.i8("Data6"), o.i8("Data7"),
            o.i8("Data8"), o.i8("Data9"), o.i8("Data10"), o.i8("Data11"),
            o.i8("Data12"), o.i8("Data13"), o.i8("Data14"), o.i8("Data15")
        );
    }
}
