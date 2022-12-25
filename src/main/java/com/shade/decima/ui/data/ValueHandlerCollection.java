package com.shade.decima.ui.data;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.path.PathElement;
import com.shade.util.NotNull;

import java.util.Collection;

public interface ValueHandlerCollection<OBJECT, CHILD> extends ValueHandler {
    @NotNull
    Collection<CHILD> getChildren(@NotNull RTTIType<?> type, @NotNull OBJECT object);

    @NotNull
    String getChildName(@NotNull RTTIType<?> type, @NotNull OBJECT object, @NotNull CHILD child);

    @NotNull
    RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull OBJECT object, @NotNull CHILD child);

    @NotNull
    PathElement getChildElement(@NotNull RTTIType<?> type, @NotNull OBJECT object, @NotNull CHILD child);
}
