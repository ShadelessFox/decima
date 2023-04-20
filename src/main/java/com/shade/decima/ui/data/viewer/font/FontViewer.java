package com.shade.decima.ui.data.viewer.font;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.Objects;

@ValueViewerRegistration(@Type(name = "FontResource"))
public class FontViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new FontViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull Editor editor) {
        final CoreEditor coreEditor = (CoreEditor) editor;
        final RTTIObject value = (RTTIObject) Objects.requireNonNull(coreEditor.getSelectedValue());
        final FontViewerPanel panel = (FontViewerPanel) component;

        panel.setObject(value, coreEditor.getInput().getProject().getContainer().getType());
    }
}
