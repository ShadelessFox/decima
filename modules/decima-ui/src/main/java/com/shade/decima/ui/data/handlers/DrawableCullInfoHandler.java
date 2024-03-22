package com.shade.decima.ui.data.handlers;

import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Field;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "drawableCullInfo", name = "Cull Info", value = {
    @Selector(field = @Field(type = "DrawableCullInfo", field = "Flags"))
})
public class DrawableCullInfoHandler extends FlagsValueHandler {
    @Nullable
    @Override
    protected String getFlagName(int flag) {
        return switch (flag) {
            case 32 -> "Cast shadows";
            default -> null;
        };
    }
}
