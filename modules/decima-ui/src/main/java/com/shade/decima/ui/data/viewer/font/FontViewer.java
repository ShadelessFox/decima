package com.shade.decima.ui.data.viewer.font;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

@ValueViewerRegistration(@Type(name = "FontResource"))
public class FontViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new FontViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        final RTTIObject value = (RTTIObject) controller.getValue();
        final FontViewerPanel panel = (FontViewerPanel) component;

        panel.setObject(value, controller.getProject().getContainer().getType());
    }
}
