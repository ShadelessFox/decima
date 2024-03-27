package com.shade.decima.ui.data.viewer.audio;

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
    @Selector(type = @Type(name = "LocalizedSimpleSoundResource")),
    @Selector(type = @Type(name = "WaveResource"), game = GameType.HZD)
})
public class AudioPlayer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new AudioPlayerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        final var panel = (AudioPlayerPanel) component;
        final var value = (RTTIObject) controller.getValue();
        panel.setInput(controller.getProject(), value);
    }
}
