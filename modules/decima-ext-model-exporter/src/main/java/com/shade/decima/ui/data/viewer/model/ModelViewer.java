package com.shade.decima.ui.data.viewer.model;

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
    @Selector(type = @Type(name = "MeshResourceBase")),
    @Selector(type = @Type(name = "ArtPartsDataResource"), game = {GameType.DS, GameType.DSDC}),
    @Selector(type = @Type(name = "ArtPartsSubModelResource"), game = {GameType.DS, GameType.DSDC}),
    @Selector(type = @Type(name = "LevelDataGame"), game = {GameType.DS, GameType.DSDC}),
    @Selector(type = @Type(name = "ArtPartsSubModelWithChildrenResource"), game = {GameType.DS, GameType.DSDC}),
    @Selector(type = @Type(name = "ObjectCollection")),
    @Selector(type = @Type(name = "StaticMeshResource")),
    @Selector(type = @Type(name = "StaticMeshInstance")),
    @Selector(type = @Type(name = "PrefabResource")),
    @Selector(type = @Type(name = "PrefabInstance")),
    @Selector(type = @Type(name = "ModelPartResource")),
    @Selector(type = @Type(name = "HumanoidBodyVariant"), game = GameType.HZD),
    @Selector(type = @Type(name = "HairModelComponentResource")),
    @Selector(type = @Type(name = "HairSkinnedMeshLod")),
    @Selector(type = @Type(name = "HairSkinnedMesh")),
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
        panel.setController((ValueController<RTTIObject>) controller);
    }
}
