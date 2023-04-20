package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.handlers.custom.PackingInfoHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(value = @Type(name = "TextureSetEntry"), id = "textureUsage", name = "Texture Usage")
public class TextureSetEntryValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject textureSetEntry = (RTTIObject) value;
            final String colorSpace = textureSetEntry.str("ColorSpace");
            final String compressMethod = textureSetEntry.str("CompressMethod");
            final int packingInfo = textureSetEntry.i32("PackingInfo");
            component.append("%s, ".formatted(colorSpace), TextAttributes.REGULAR_BOLD_ATTRIBUTES);
            component.append("%s, ".formatted(compressMethod), TextAttributes.REGULAR_ATTRIBUTES);
            PackingInfoHandler.INSTANCE.getDecorator(type).decorate(packingInfo, component);
        };
    }
}
