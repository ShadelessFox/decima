package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Field;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import java.util.StringJoiner;

@ValueHandlerRegistration(id = "enabledTechniques", name = "Enabled techniques", value = {
    @Selector(field = @Field(type = "RenderTechniqueSet", field = "InitiallyEnabledTechniquesMask"))
})
public class RenderTechniqueSetEnabledTechniquesHandler extends NumberValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final int mask = ((Number) value).intValue();
            final StringJoiner joiner = new StringJoiner(", ");

            for (int i = 0; i < 32; i++) {
                if ((mask & (1 << i)) != 0) {
                    joiner.add(String.valueOf(i));
                }
            }

            component.append(joiner.toString(), TextAttributes.REGULAR_ATTRIBUTES);
        };
    }
}
