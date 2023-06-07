package com.shade.decima.ui.data.viewer.wwise;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Selector;
import com.shade.decima.ui.data.registry.ValueHandlerRegistration.Type;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

@ValueViewerRegistration({
    @Selector(type = @Type(name = "WwiseBankResource"), game = {GameType.DS, GameType.DSDC}),
    @Selector(type = @Type(name = "WwiseWemResource"), game = {GameType.DS, GameType.DSDC}),
    @Selector(type = @Type(name = "LocalizedSimpleSoundResource"), game = {GameType.DS, GameType.DSDC})
})
public class WwiseViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new WwiseViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        final var panel = (WwiseViewerPanel) component;
        final var value = (RTTIObject) controller.getValue();
        panel.setInput(controller.getProject(), value);
    }
}
