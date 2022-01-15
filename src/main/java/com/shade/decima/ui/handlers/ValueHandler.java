package com.shade.decima.ui.handlers;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

public interface ValueHandler {
    /**
     * Returns the value inlined in the node's name, or none (name of the type is used instead)
     */
    @Nullable
    String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value);
}
