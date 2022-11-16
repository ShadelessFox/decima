package com.shade.decima.ui.data.viewer;

import com.shade.decima.ui.controls.HexTextArea;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;

@ValueViewerRegistration({
    @Type(name = "Array<int8>"),
    @Type(name = "Array<uint8>")
})
public class BinaryViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        final JScrollPane pane = new JScrollPane(new HexTextArea());
        pane.setBorder(null);

        return pane;
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull Editor editor) {
        final HexTextArea area = (HexTextArea) ((JScrollPane) component).getViewport().getView();
        final byte[] data = (byte[]) ((CoreEditor) editor).getSelectedValue();

        if (data != null) {
            area.setData(data);
        }
    }
}
