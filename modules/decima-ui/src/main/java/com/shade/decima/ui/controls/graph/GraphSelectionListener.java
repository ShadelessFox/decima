package com.shade.decima.ui.controls.graph;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

public interface GraphSelectionListener {
    default void nodeDoubleClicked(@NotNull RTTIObject object) {
        // do nothing by default
    }
}
