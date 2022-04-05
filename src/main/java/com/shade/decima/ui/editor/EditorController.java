package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public interface EditorController {
    @Nullable
    RTTIType<?> getSelectedType();

    @Nullable
    Object getSelectedValue();

    @NotNull
    Project getProject();
}
