package com.shade.decima.ui.data;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public interface ValueHandler {
    @Nullable
    Decorator getDecorator(@NotNull RTTIType<?> type);

    @Nullable
    default Icon getIcon(@NotNull RTTIType<?> type) {
        return null;
    }

    @Nullable
    default String getString(@NotNull RTTIType<?> type, @NotNull Object value) {
        return null;
    }

    interface Decorator {
        void decorate(@NotNull Object value, @NotNull ColoredComponent component);
    }
}
