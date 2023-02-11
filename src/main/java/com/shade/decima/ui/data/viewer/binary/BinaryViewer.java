package com.shade.decima.ui.data.viewer.binary;

import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.Objects;

@ValueViewerRegistration(@Type(type = byte[].class))
public class BinaryViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new BinaryViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull Editor editor) {
        final var panel = (BinaryViewerPanel) component;
        final var data = (byte[]) Objects.requireNonNull(((CoreEditor) editor).getSelectedValue());
        panel.setInput(data);
    }
}
