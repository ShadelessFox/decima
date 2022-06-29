package com.shade.decima.ui.handler;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.controls.ColoredComponent;

public interface ValueHandler {
    /**
     * Appends the value inlined in the node's name, or none (name of the type is used instead)
     */
    void appendInlineValue(@NotNull RTTIType<?> type, @NotNull Object value, @NotNull ColoredComponent component);

    boolean hasInlineValue();
}
