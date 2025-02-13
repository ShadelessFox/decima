package com.shade.decima.ui.data.viewer.model.dmf.data;

import com.shade.decima.ui.data.viewer.model.dmf.nodes.DMFMapTile;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public final class DMFTileData {
    public final Map<String, DMFMapTile.TileTextureInfo> textures;
    public final Point gridCoordinate;
    public Vector3fc bboxMin;
    public Vector3fc bboxMax;

    private DMFTileData(Map<String, DMFMapTile.TileTextureInfo> textures, Point gridCoordinate, Vector3f bboxMin, Vector3f bboxMax) {
        this.textures = textures;
        this.gridCoordinate = gridCoordinate;
        this.bboxMin = bboxMin;
        this.bboxMax = bboxMax;
    }

    public DMFTileData(Point gridCoordinate) {
        this(new HashMap<>(), gridCoordinate, null, null);
    }

    public String toString() {
        return "TileData[" +
            "textures=" + textures + ", " +
            "gridCoordinate=" + gridCoordinate + ", " +
            "bboxMin=" + bboxMin + ", " +
            "bboxMax=" + bboxMax + ']';
    }
}
