package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DMFMapTile extends DMFNode {
    int[] gridCoordinate;
    float[] bboxMin;
    float[] bboxMax;
    Map<String, DMFMapTile.TileTextureInfo> textures = new HashMap<>();

    public DMFMapTile(@NotNull String name) {
        super(name, DMFNodeType.MAP_TILE);
    }

    public static final class TileTextureInfo {
        public final Map<String, TileTextureChannelInfo> channels = new HashMap<>();
        public Integer textureId = null;

        public static class TileTextureChannelInfo {
            public String usage;
            public float minRange;
            public float maxRange;

            public TileTextureChannelInfo(String usage, float minRange, float maxRange) {
                this.usage = usage;
                this.minRange = minRange;
                this.maxRange = maxRange;
            }
        }
    }
}
