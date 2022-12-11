package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeNumber;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(value = @Type(type = RTTITypeNumber.class), name = "Decimal")
public class NumberValueHandler implements ValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(getString(type, value), CommonTextAttributes.NUMBER_ATTRIBUTES);
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        if (((RTTITypeNumber<?>) type).isDecimal()) {
            return UIManager.getIcon("CoreEditor.decimalIcon");
        } else {
            return UIManager.getIcon("CoreEditor.integerIcon");
        }
    }

    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        if (((RTTITypeNumber<?>) type).isSigned()) {
            return value.toString();
        }

        if (value instanceof Byte v) {
            return String.valueOf(Byte.toUnsignedInt(v));
        } else if (value instanceof Short v) {
            return String.valueOf(Short.toUnsignedInt(v));
        } else if (value instanceof Integer v) {
            return Integer.toUnsignedString(v);
        } else if (value instanceof Long v) {
            return Long.toUnsignedString(v);
        } else {
            return value.toString();
        }
    }
}
