package com.shade.decima.ui.data.viewer.binary;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;

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
        final var controller = ((CoreEditor) editor).<byte[]>getValueController(ValueController.EditType.INLINE);

        if (controller != null) {
            panel.setController(controller);
        }
    }
}
