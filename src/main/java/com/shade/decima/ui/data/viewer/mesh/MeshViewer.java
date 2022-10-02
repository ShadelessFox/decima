package com.shade.decima.ui.data.viewer.mesh;

import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;

public class MeshViewer implements ValueViewer {
    public static final MeshViewer INSTANCE = new MeshViewer();

    @NotNull
    @Override
    public JComponent createComponent() {
        return new MeshViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull Editor editor) {
        final MeshViewerPanel panel = (MeshViewerPanel) component;
        panel.setInput((CoreEditor) editor);
    }
}
