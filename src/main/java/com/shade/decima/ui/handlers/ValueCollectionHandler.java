package com.shade.decima.ui.handlers;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.util.Collection;

public interface ValueCollectionHandler<OBJECT, CHILD> extends ValueHandler {
    @NotNull
    Collection<CHILD> getChildren(@NotNull RTTIType<?> type, @NotNull OBJECT object);

    @NotNull
    String getChildName(@NotNull RTTIType<?> type, @NotNull OBJECT object, @NotNull CHILD child);

    @NotNull
    Object getChildValue(@NotNull RTTIType<?> type, @NotNull OBJECT object, @NotNull CHILD child);

    @NotNull
    RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull OBJECT object, @NotNull CHILD child);
}
