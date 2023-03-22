package com.shade.decima.ui.data.viewer.wwise;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.editors.Editor;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.Objects;

@ValueViewerRegistration({
    @Type(name = "WwiseBankResource", game = {GameType.DS, GameType.DSDC}),
    @Type(name = "WwiseWemResource", game = {GameType.DS, GameType.DSDC})
})
public class WwiseViewer implements ValueViewer {
    private WwiseViewerPanel panel;

    @NotNull
    @Override
    public JComponent createComponent() {
        return panel = new WwiseViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull Editor editor) {
        final var core = (CoreEditor) editor;
        final var value = Objects.requireNonNull(((RTTIObject) core.getSelectedValue()));
        panel.setInput(core.getInput().getProject(), value);
    }

    @Override
    public void dispose() {
        panel.close();
    }
}
