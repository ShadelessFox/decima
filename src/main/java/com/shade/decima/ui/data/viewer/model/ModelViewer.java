package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.registry.ValueViewerRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

@ValueViewerRegistration({
    @Type(name = "MeshResourceBase", game = {GameType.DS, GameType.HZD}),
    @Type(name = "ArtPartsDataResource", game = GameType.DS),
    @Type(name = "ObjectCollection", game = {GameType.DS, GameType.HZD}),
    @Type(name = "StaticMeshResource", game = {GameType.DS, GameType.HZD}),
    @Type(name = "SkinnedModelResource", game = GameType.HZD),
    @Type(name = "StreamingTileResource", game = GameType.HZD),
    @Type(name = "TileBasedStreamingStrategyResource", game = GameType.HZD),
    @Type(name = "ControlledEntityResource", game = GameType.HZD),
    @Type(name = "Terrain", game = GameType.HZD)
})
public class ModelViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new ModelViewerPanel();
    }

    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        final ModelViewerPanel panel = (ModelViewerPanel) component;
        panel.setInput(controller);
    }
}
