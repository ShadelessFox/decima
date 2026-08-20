package com.shade.decima.model.viewer;

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
    @Selector(type = @Type(name = "MeshResourceBase"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
    @Selector(type = @Type(name = "ArtPartsDataResource"), game = {GameType.DS, GameType.DSDC}),
    @Selector(type = @Type(name = "ArtPartsSubModelResource"), game = {GameType.DS, GameType.DSDC}),
    @Selector(type = @Type(name = "ArtPartsSubModelWithChildrenResource"), game = {GameType.DS, GameType.DSDC}),
    @Selector(type = @Type(name = "ObjectCollection"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
    @Selector(type = @Type(name = "StaticMeshResource"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
    @Selector(type = @Type(name = "StaticMeshInstance"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
    @Selector(type = @Type(name = "PrefabResource"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
    @Selector(type = @Type(name = "PrefabInstance"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
    @Selector(type = @Type(name = "ModelPartResource"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
    @Selector(type = @Type(name = "HumanoidBodyVariant"), game = GameType.HZD),
    @Selector(type = @Type(name = "HairModelComponentResource"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
    @Selector(type = @Type(name = "HairSkinnedMeshLod"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
    @Selector(type = @Type(name = "HairSkinnedMesh"), game = {GameType.DS, GameType.DSDC, GameType.HZD}),
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
