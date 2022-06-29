package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.controls.ColoredComponent;
import com.shade.decima.ui.controls.TextAttributes;
import com.shade.decima.ui.handler.ValueHandler;

public class SignedNumberValueHandler implements ValueHandler {
    public static final SignedNumberValueHandler INSTANCE = new SignedNumberValueHandler();

    private SignedNumberValueHandler() {
    }

    @Override
    public void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component) {
        component.append(String.valueOf(value), TextAttributes.BLUE_ATTRIBUTES);
    }

    @Override
    public boolean hasInlineValue() {
        return true;
    }
}
