package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

@ValueHandlerRegistration(id = "resource", name = "Resource", value = {
    @Type(name = "Resource", game = GameType.HZD),
    @Type(name = "ResourceWithName", game = GameType.DS)
})
public class ObjectWithNameValueHandler extends ObjectValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(((RTTIObject) value).str("Name"), TextAttributes.REGULAR_ATTRIBUTES);
    }
}
