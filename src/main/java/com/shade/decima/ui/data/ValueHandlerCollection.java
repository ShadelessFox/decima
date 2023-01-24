package com.shade.decima.ui.data;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.util.NotNull;

public interface ValueHandlerCollection<OBJECT, ELEMENT extends RTTIPathElement> extends ValueHandler {
    @NotNull
    ELEMENT[] getElements(@NotNull RTTIType<?> type, @NotNull OBJECT object);

    @NotNull
    String getElementName(@NotNull RTTIType<?> type, @NotNull OBJECT object, @NotNull ELEMENT element);

    @NotNull
    RTTIType<?> getElementType(@NotNull RTTIType<?> type, @NotNull OBJECT object, @NotNull ELEMENT element);
}
