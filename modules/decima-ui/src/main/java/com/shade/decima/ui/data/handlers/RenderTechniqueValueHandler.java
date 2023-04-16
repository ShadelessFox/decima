package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "technique", name = "Technique", value = {
    @Type(name = "RenderTechnique"),
    @Type(name = "RenderTechniqueSet")
})
public class RenderTechniqueValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject obj = (RTTIObject) value;
            final String text;
            if (obj.type().getTypeName().equals("RenderTechniqueSet")) {
                text = "%s, %s".formatted(obj.get("Type").toString(), obj.get("EffectType").toString());
            } else {
                text = "%s".formatted(obj.get("TechniqueType").toString());
            }
            component.append(text, TextAttributes.REGULAR_ATTRIBUTES);
        };
    }
}
