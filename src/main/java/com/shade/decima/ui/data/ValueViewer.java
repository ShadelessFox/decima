package com.shade.decima.ui.data;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.editors.EditorController;

import javax.swing.*;

public interface ValueViewer {
    @NotNull
    JComponent createComponent();

    void refresh(@NotNull JComponent component, @NotNull EditorController controller);
}
