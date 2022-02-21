package com.shade.decima.ui.handlers.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.handlers.ValueHandler;

public class NumberValueHandler implements ValueHandler {
    public static final NumberValueHandler INSTANCE = new NumberValueHandler();

    private NumberValueHandler() {
    }

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        return "<font color=#1750eb>%s</font>".formatted(value);
    }
}
