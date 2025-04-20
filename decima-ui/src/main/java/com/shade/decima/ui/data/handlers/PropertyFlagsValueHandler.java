package com.shade.decima.ui.data.handlers;

import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Field;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "propertyFlags", name = "Property Flags", value = {
    @Selector(field = @Field(type = "Property", field = "Flags"))
})
public class PropertyFlagsValueHandler extends FlagsValueHandler {
    @Nullable
    @Override
    public String getFlagName(int flag) {
        return switch (flag) {
            case 2 -> "Replicated";
            case 4 -> "Persistent";
            default -> null;
        };
    }
}
