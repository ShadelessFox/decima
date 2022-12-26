package com.shade.decima.ui.data.handlers.custom;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.messages.impl.ShaderResource.DXBCShader.RDEFChunk.ResourceBinding;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.handlers.ObjectValueHandler;
import com.shade.decima.ui.data.handlers.StringValueHandler;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.util.NotNull;

@ValueHandlerRegistration(value = @Type(type = ResourceBinding.class), id = "resourceBinding", name = "Pretty view", order = 100)
public class ResourceBindingHandler extends ObjectValueHandler {
    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        return ((RTTIObject) value).<ResourceBinding>cast().name;
    }

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(((RTTIObject) value).<ResourceBinding>cast().name, CommonTextAttributes.STRING_TEXT_ATTRIBUTES);
    }
}