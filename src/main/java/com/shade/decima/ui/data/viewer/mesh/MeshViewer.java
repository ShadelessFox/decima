package com.shade.decima.ui.data.viewer.mesh;

import com.shade.decima.model.base.GameType;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;

@ValueViewerRegistration({
    @Type(name = "MeshResourceBase", game = GameType.DS),
    @Type(name = "ArtPartsDataResource", game = GameType.DS),
    @Type(name = "ObjectCollection", game = GameType.DS)
})
public class MeshViewer implements ValueViewer {
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
