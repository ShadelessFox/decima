package com.shade.decima.ui.data.viewer.model.model.data;

import com.shade.util.NotNull;

public interface Accessor {
    @NotNull
    ElementType getElementType();

    @NotNull
    ComponentType getComponentType();

    int getElementCount();

    int getComponentCount();
}
