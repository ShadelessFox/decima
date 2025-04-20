package com.shade.decima.ui.data.handlers;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;

public class DefaultValueHandler implements ValueHandler {
    public static final DefaultValueHandler INSTANCE = new DefaultValueHandler();

    private DefaultValueHandler() {
    }

    @NotNull
    @Override
    public Decorator getDecorator(@NotNull RTTIType<?> type) {
        return (value, component) -> component.append(value.toString(), TextAttributes.REGULAR_ATTRIBUTES);
    }
}
