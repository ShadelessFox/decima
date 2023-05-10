package com.shade.decima.ui.data.viewer.binary;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

@ValueViewerRegistration(@Type(type = byte[].class))
public class BinaryViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new BinaryViewerPanel();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        final var panel = (BinaryViewerPanel) component;
        panel.setController((ValueController<byte[]>) controller);
    }
}
