package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "programParameter", name = "Program Parameter", value = {
    @Selector(type = @Type(name = "ProgramParameter"))
})
public class ProgramParameterHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject object = (RTTIObject) value;
            component.append(object.str("TypeName"), CommonTextAttributes.IDENTIFIER_ATTRIBUTES);
            component.append(" ", TextAttributes.REGULAR_ATTRIBUTES);
            component.append(object.str("Name"), TextAttributes.REGULAR_ATTRIBUTES);
        };
    }
}
