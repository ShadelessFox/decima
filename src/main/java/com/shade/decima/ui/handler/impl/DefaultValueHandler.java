package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.controls.ColoredComponent;
import com.shade.decima.ui.controls.TextAttributes;
import com.shade.decima.ui.handler.ValueHandler;

public class DefaultValueHandler implements ValueHandler {
    public static final DefaultValueHandler INSTANCE = new DefaultValueHandler();

    private DefaultValueHandler() {
    }

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        component.append(String.valueOf(value), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }
}
