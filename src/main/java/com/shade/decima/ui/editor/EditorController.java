package com.shade.decima.ui.editor;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import javax.swing.*;

public interface EditorController {
    @Nullable
    RTTIType<?> getSelectedType();

    @Nullable
    Object getSelectedValue();

    void setSelectedValue(@Nullable Object value);

    @NotNull
    JComponent getFocusComponent();
}
