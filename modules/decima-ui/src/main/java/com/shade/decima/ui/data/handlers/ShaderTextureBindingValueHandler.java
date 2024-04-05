package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(id = "textureBinding", name = "Texture Binding", value = {
    @Selector(type = @Type(name = "ShaderTextureBinding"))
})
public class ShaderTextureBindingValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject obj = (RTTIObject) value;
            final int packedData = obj.i32("PackedData");
            final boolean isTextureSet = (packedData & 3) == 2;
            final String texturePurpose = PackingInfoHandler.getUsage(packedData >>> 2 & 0xf);
            NameHashValueHandler.INSTANCE.getDecorator(type).decorate(obj.i32("BindingNameHash"), component);
            component.append(", type = ", TextAttributes.REGULAR_ATTRIBUTES);
            if (isTextureSet) {
                component.append("TextureSet, usage = ", TextAttributes.REGULAR_ATTRIBUTES);
                component.append(texturePurpose, TextAttributes.REGULAR_BOLD_ATTRIBUTES);
                component.append(", ", TextAttributes.REGULAR_ATTRIBUTES);
            } else {
                component.append("Texture, ", TextAttributes.REGULAR_ATTRIBUTES);
            }
            ReferenceValueHandler.INSTANCE.getDecorator(type).decorate(obj.get("TextureResource"), component);
        };
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.textureIcon");
    }
}
