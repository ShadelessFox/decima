package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(value = {
    @Selector(type = @Type(type = RTTIReference.class))
})
public class ReferenceValueHandler implements ValueHandler {
    public static final ReferenceValueHandler INSTANCE = new ReferenceValueHandler();

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> {
            if (value instanceof RTTIReference.External ref) {
                component.append(ref.path(), TextAttributes.REGULAR_ATTRIBUTES);
                component.append(" : ", TextAttributes.REGULAR_ATTRIBUTES);
                component.append(RTTIUtils.uuidToString(ref.uuid()), TextAttributes.REGULAR_ATTRIBUTES);
                component.append(" (" + ref.kind() + ")", TextAttributes.GRAYED_ATTRIBUTES);
            } else if (value instanceof RTTIReference.Internal ref) {
                component.append(RTTIUtils.uuidToString(ref.uuid()), TextAttributes.REGULAR_ATTRIBUTES);
                component.append(" (" + ref.kind() + ")", TextAttributes.GRAYED_ATTRIBUTES);
            } else {
                component.append("none", TextAttributes.REGULAR_ATTRIBUTES);
            }
        };
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.referenceIcon");
    }

    @Nullable
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        if (value instanceof RTTIReference.External ref) {
            return ref.path();
        } else {
            return null;
        }
    }
}
