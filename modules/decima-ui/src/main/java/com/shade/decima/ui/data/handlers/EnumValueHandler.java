package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

import javax.swing.*;

@ValueHandlerRegistration(value = {
    @Selector(type = @Type(type = RTTITypeEnum.Constant.class)),
})
public class EnumValueHandler implements ValueHandler {
    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(value.toString(), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @NotNull
    @Override
    public Icon getIcon(@NotNull RTTIType<?> type) {
        return UIManager.getIcon("Node.enumIcon");
    }

    @NotNull
    @Override
    public String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        return value.toString();
    }
}
