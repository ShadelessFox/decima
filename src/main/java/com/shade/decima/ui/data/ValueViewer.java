package com.shade.decima.ui.data;

import com.shade.platform.Disposable;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;

public interface ValueViewer extends Disposable {
    @NotNull
    JComponent createComponent();

    void refresh(@NotNull JComponent component, @NotNull Editor editor);

    @Override
    default void dispose() {
        // do nothing by default
    }
}
