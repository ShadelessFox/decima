package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.handler.ValueHandler;

public class DefaultValueHandler implements ValueHandler {
    public static final DefaultValueHandler INSTANCE = new DefaultValueHandler();

    private DefaultValueHandler() {
    }

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        return String.valueOf(value);
    }
}
