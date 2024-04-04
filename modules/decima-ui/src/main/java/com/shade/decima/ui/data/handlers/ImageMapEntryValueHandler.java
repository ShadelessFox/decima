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

@ValueHandlerRegistration(id = "imageMapEntry", name = "Image ID", value = {
    @Selector(type = @Type(name = "ImageMapEntry"))
})
public class ImageMapEntryValueHandler extends ObjectValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(((RTTIObject) value).str("ID"), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.textureIcon");
    }
}
