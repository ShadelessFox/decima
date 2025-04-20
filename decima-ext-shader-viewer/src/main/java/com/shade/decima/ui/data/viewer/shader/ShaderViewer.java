package com.shade.decima.ui.data.viewer.shader;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

@ValueViewerRegistration({
    @Selector(type = @Type(name = "ShaderResource"))
})
public class ShaderViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new ShaderViewerPanel();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        ((ShaderViewerPanel) component).setInput((ValueController<RTTIObject>) controller);
    }
}
