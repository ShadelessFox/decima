package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

@ValueHandlerRegistration(id = "assetPath", name = "Asset path", value = {
    @Selector(type = @Type(name = "AssetPath"))
})
public class AssetPathValueHandler extends ObjectValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(getText(type, value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        return ((RTTIObject) value).str("Path");
    }
}
