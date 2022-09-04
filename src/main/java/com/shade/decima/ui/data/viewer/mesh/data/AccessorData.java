package com.shade.decima.ui.data.viewer.mesh.data;

import com.shade.util.NotNull;

public interface AccessorData {
    @NotNull
    ElementType getElementType();

    @NotNull
    ComponentType getComponentType();

    int getElementCount();

    int getComponentCount();
}
