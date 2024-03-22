package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

@ValueHandlerRegistration(id = "resource", name = "Resource", value = {
    @Selector(type = @Type(name = "Resource"), game = GameType.HZD),
    @Selector(type = @Type(name = "Property"), game = GameType.HZD),
    @Selector(type = @Type(name = "ResourceWithName"), game = GameType.DS),
    @Selector(type = @Type(name = "ResourceWithName"), game = GameType.DSDC)
})
public class ObjectWithNameValueHandler extends ObjectValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(getText(type, value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        return ((RTTIObject) value).str("Name");
    }
}
