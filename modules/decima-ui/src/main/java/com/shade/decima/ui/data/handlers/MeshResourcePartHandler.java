package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(id = "meshPart", name = "Mesh part", value = {
    @Selector(type = @Type(name = "MultiMeshResourcePart")),
    @Selector(type = @Type(name = "LodMeshResourcePart"))
})
public class MeshResourcePartHandler extends ObjectValueHandler {
    @Nullable
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            final RTTIObject obj = (RTTIObject) value;
            ReferenceValueHandler.INSTANCE.getDecorator(type).decorate(obj.get("Mesh"), component);
        };
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.modelIcon");
    }
}
