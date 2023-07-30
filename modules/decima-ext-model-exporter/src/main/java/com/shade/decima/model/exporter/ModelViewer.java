package com.shade.decima.model.exporter;

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
    @Selector(type = @Type(name = "MeshResourceBase"), game = {GameType.DS, GameType.HZD}),
    @Selector(type = @Type(name = "ArtPartsDataResource"), game = GameType.DS),
    @Selector(type = @Type(name = "ObjectCollection"), game = {GameType.DS, GameType.HZD}),
    @Selector(type = @Type(name = "StaticMeshResource"), game = {GameType.DS, GameType.HZD}),
    @Selector(type = @Type(name = "SkinnedModelResource"), game = GameType.HZD),
    @Selector(type = @Type(name = "StreamingTileResource"), game = GameType.HZD),
    @Selector(type = @Type(name = "TileBasedStreamingStrategyResource"), game = GameType.HZD),
    @Selector(type = @Type(name = "ControlledEntityResource"), game = GameType.HZD),
    @Selector(type = @Type(name = "Terrain"), game = GameType.HZD)
})
public class ModelViewer implements ValueViewer {
    @NotNull
    @Override
    public JComponent createComponent() {
        return new ModelViewerPanel();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void refresh(@NotNull JComponent component, @NotNull ValueController<?> controller) {
        final ModelViewerPanel panel = (ModelViewerPanel) component;
        panel.setInput((ValueController<RTTIObject>) controller);
    }
}
