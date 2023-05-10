package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "size", name = "Size", value = {
    @Type(name = "ISize"),
    @Type(name = "FSize")
})
public class SizeValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject obj = (RTTIObject) value;
            final String text = "%sx%s".formatted(obj.get("Width"), obj.get("Height"));

            component.append(text, TextAttributes.REGULAR_ATTRIBUTES);
        };
    }
}
