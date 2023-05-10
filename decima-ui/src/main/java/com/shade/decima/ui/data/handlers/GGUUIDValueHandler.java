package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(value = @Type(name = "GGUUID"), id = "guid", name = "GUID")
public class GGUUIDValueHandler extends ObjectValueHandler {
    public static final GGUUIDValueHandler INSTANCE = new GGUUIDValueHandler();

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append("{%s}".formatted(getString(type, value)), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.uuidIcon");
    }

    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        return RTTIUtils.uuidToString((RTTIObject) value);
    }
}
