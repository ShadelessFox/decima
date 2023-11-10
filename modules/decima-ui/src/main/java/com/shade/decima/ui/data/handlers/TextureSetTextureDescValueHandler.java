package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "textureSetTextureDesc", name = "Texture Description", value = {
    @Selector(type = @Type(name = "TextureSetTextureDesc"))
})
public class TextureSetTextureDescValueHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject object = (RTTIObject) value;

            component.append(object.str("TextureType"), TextAttributes.REGULAR_ATTRIBUTES);

            if (!object.str("Path").isEmpty()) {
                component.append(" " + object.str("Path"), TextAttributes.GRAYED_SMALL_ATTRIBUTES);
            }
        };
    }
}
