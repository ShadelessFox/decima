package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.ui.data.handlers.custom.PackingInfoHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(value = @Type(name = "ShaderTextureBinding"), id="textureBinding", name = "Texture Binding")
public class ShaderTextureBindingValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject obj = (RTTIObject) value;
            final int packedData = obj.i32("PackedData");
            final Boolean isTextureSet = (packedData & 0x3) == 2;
            final String texturePurpose = PackingInfoHandler.getPurpose(packedData >>> 2 & 0xf);
            component.append("{0x%X}, ".formatted(packedData >>> 6), TextAttributes.REGULAR_ATTRIBUTES);
            if (isTextureSet) {
                component.append("TextureSet: usage = ", TextAttributes.REGULAR_ATTRIBUTES);
                component.append(texturePurpose, TextAttributes.REGULAR_BOLD_ATTRIBUTES);
            } else {
                component.append("Texture:", TextAttributes.REGULAR_ATTRIBUTES);
            }
            if (obj.get("TextureResource") instanceof RTTIReference.External ref) {
                component.append(" path = ", TextAttributes.REGULAR_ATTRIBUTES);
                component.append(ref.path().substring(0, ref.path().lastIndexOf('/') + 1), TextAttributes.REGULAR_ATTRIBUTES);
                component.append(ref.path().substring(ref.path().lastIndexOf('/') + 1), TextAttributes.REGULAR_BOLD_ATTRIBUTES);
                component.append(", uuid = ", TextAttributes.REGULAR_ATTRIBUTES);
                GGUUIDValueHandler.INSTANCE.getDecorator(ref.uuid().type()).decorate(ref.uuid(), component);
            } else if (obj.get("TextureResource") instanceof RTTIReference.Internal ref) {
                component.append(" uuid = ", TextAttributes.REGULAR_ATTRIBUTES);
                GGUUIDValueHandler.INSTANCE.getDecorator(ref.uuid().type()).decorate(ref.uuid(), component);
            }
        };
    }
}
