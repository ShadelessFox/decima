package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.util.NotNull;

@ValueHandlerRegistration(id = "namedObject", name = "Named Object", order = 50, value = {
    @Selector(type = @Type(name = "Resource"), game = GameType.HZD),
    @Selector(type = @Type(name = "Property"), game = GameType.HZD),
    @Selector(type = @Type(name = "ResourceWithName"), game = GameType.DS),
    @Selector(type = @Type(name = "ResourceWithName"), game = GameType.DSDC),
    @Selector(type = @Type(name = "OrientationHelper"))
})
public class ObjectWithNameValueHandler extends ObjectValueHandler {
    public static final ObjectWithNameValueHandler INSTANCE = new ObjectWithNameValueHandler();

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append("\"%s\"".formatted(getText(type, value)), CommonTextAttributes.STRING_TEXT_ATTRIBUTES);
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        return ((RTTIObject) value).str("Name");
    }
}
