package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.messages.ds.DSVertexArrayResourceHandler.HwVertexStreamElement;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@ValueHandlerRegistration(id = "vertexStreamElement", name = "Vertex Stream Element", value = {
    @Selector(type = @Type(type = HwVertexStreamElement.class)),
})
public class VertexStreamElement extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final HwVertexStreamElement element = ((RTTIObject) value).cast();

            component.append(element.storageType.toString(), TextAttributes.REGULAR_ATTRIBUTES);
            component.append(" ", TextAttributes.REGULAR_ATTRIBUTES);
            component.append(element.type.toString(), TextAttributes.REGULAR_BOLD_ATTRIBUTES);
            component.append(" +" + element.offset, TextAttributes.REGULAR_ATTRIBUTES);
        };
    }
}
