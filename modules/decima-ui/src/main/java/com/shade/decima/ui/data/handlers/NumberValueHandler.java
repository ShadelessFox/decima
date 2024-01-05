package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeNumber;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

@ValueHandlerRegistration(name = "Decimal", order = 50, value = {
    @Selector(type = @Type(type = Number.class))
})
public class NumberValueHandler implements ValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(getText(type, value), CommonTextAttributes.NUMBER_ATTRIBUTES);
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        if (((RTTITypeNumber<?>) type).isDecimal()) {
            return UIManager.getIcon("Node.decimalIcon");
        } else {
            return UIManager.getIcon("Node.integerIcon");
        }
    }

    @NotNull
    @Override
    public String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        return toString((RTTITypeNumber<?>) type, value);
    }

    public static String toString(@NotNull RTTITypeNumber<?> type, @NotNull Object value) {
        if (type.isSigned()) {
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
