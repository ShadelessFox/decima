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

@ValueHandlerRegistration(id = "programResourceEntryPoint", name = "Program Resource Entry Point", value = {
    @Selector(type = @Type(name = "ProgramResourceEntryPoint"))
})
public class ProgramResourceEntryPointHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject object = (RTTIObject) value;
            component.append(object.str("EntryPoint"), TextAttributes.REGULAR_ATTRIBUTES);
            component.append("(", TextAttributes.REGULAR_ATTRIBUTES);

            final RTTIObject[] parameters = object.objs("InputParameters");
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) {
                    component.append(", ", TextAttributes.REGULAR_ATTRIBUTES);
                }

                // ProgramParameterHandler#getDecorator
                component.append(parameters[i].str("TypeName"), CommonTextAttributes.IDENTIFIER_ATTRIBUTES);
                component.append(" ", TextAttributes.REGULAR_ATTRIBUTES);
                component.append(parameters[i].str("Name"), TextAttributes.REGULAR_ATTRIBUTES);
            }

            component.append(")", TextAttributes.REGULAR_ATTRIBUTES);
        };
    }
}
