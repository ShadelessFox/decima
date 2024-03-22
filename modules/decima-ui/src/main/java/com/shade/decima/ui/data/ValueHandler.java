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

    /**
     * Returns the text representation of the given value that can be copied to the clipboard.
     *
     * @param type  The type of the value.
     * @param value The value to convert.
     * @return The text representation of the given value.
     */
    @Nullable
    default String getText(@NotNull RTTIType<?> type, @NotNull Object value) {
        return null;
    }

    interface Decorator {
        void decorate(@NotNull Object value, @NotNull ColoredComponent component);

        default boolean needsGap() {
            return true;
        }
    }
}
