package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Field;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import java.util.StringJoiner;

@ValueHandlerRegistration(id = "availableTechniques", name = "Available techniques", value = {
    @Selector(field = @Field(type = "RenderTechniqueSet", field = "AvailableTechniquesMask"))
})
public class RenderTechniqueSetAvailableTechniquesHandler extends NumberValueHandler {
    // Note: A copy of ERenderTechniqueType. Please remove me once we have access to RTTITypeRegistry
    private static final String[] RENDER_TECHNIQUE_TYPES = {
        "Direct",
        "Unlit",
        "DepthOnly",
        "MaskedDepthOnly",
        "Deferred",
        "DeferredEmissive",
        "DeferredTransAcc",
        "DeferredTrans",
        "CustomDeferredBackground",
        "CustomDeferred",
        "CustomDeferredNormalRead",
        "CustomDeferredDepthWrite",
        "DeferredSimplified",
        "HalfDepthOnly",
        "LightSampling",
        "CustomForward",
        "Transparency",
        "ForwardBackground",
        "ForwardWaterFromBelow",
        "ForwardHalfRes",
        "ForwardQuarterRes",
        "ForwardMotionVectors",
        "ForwardForeground",
        "VolumeLightAmount",
        "Shadowmap"
    };

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final int mask = ((Number) value).intValue();
            final StringJoiner joiner = new StringJoiner(", ");

            for (int i = 0; i < RENDER_TECHNIQUE_TYPES.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    joiner.add(RENDER_TECHNIQUE_TYPES[i]);
                }
            }

            component.append(joiner.toString(), TextAttributes.REGULAR_ATTRIBUTES);
        };
    }
}
