package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(id = "texture", name = "Texture", value = {
    @Selector(type = @Type(name = "Texture")),
    @Selector(type = @Type(name = "MenuStreamingTexture")),
    @Selector(type = @Type(name = "UITextureBindingOverride"))
})
public class TextureValueHandler extends ObjectValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append("\"%s\"".formatted(getText(type, value)), CommonTextAttributes.STRING_TEXT_ATTRIBUTES);
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        final RTTIObject obj = (RTTIObject) value;
        final String str;
        switch (type.getTypeName()) {
            case "MenuStreamingTexture", "UITextureBindingOverride" -> {
                str = obj.str("TextureName");
            }
            default -> str = obj.str("Name");
        }
        return str;
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.textureIcon");
    }
}
