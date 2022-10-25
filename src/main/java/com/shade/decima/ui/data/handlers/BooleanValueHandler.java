package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(@Type(name = "bool"))
public class BooleanValueHandler implements ValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(value.toString(), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("CoreEditor.booleanIcon");
    }

    @Nullable
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        return value.toString();
    }
}
