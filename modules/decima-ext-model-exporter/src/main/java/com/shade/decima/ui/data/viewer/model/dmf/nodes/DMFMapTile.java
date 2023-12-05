package com.shade.decima.ui.data.viewer.model.dmf.nodes;

import com.shade.util.NotNull;
import org.joml.Vector2ic;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.Map;

public class DMFMapTile extends DMFNode {
    public Vector2ic gridCoordinate;
    public Vector3fc bboxMin;
    public Vector3fc bboxMax;
    public Map<String, DMFMapTile.TileTextureInfo> textures = new HashMap<>();

    public DMFMapTile(@NotNull String name) {
        super(name, DMFNodeType.MAP_TILE);
    }

    public record TileTextureInfo(int textureId, @NotNull Map<String, TileTextureChannelInfo> channels) {
        @Override
        public int textureId() {
            return textureId;
        }

        @Override
        public Map<String, TileTextureChannelInfo> channels() {
            return channels;
        }
    }

    public record TileTextureChannelInfo(@NotNull String usage, float minRange, float maxRange) {
        @Override
        public String usage() {
            return usage;
        }

        @Override
        public float minRange() {
            return minRange;
        }

        @Override
        public float maxRange() {
            return maxRange;
        }
    }
}
