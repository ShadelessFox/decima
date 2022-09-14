package com.shade.decima.ui.data.viewer.mesh.gltf;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GltfFile {
    public GltfAsset asset;
    public Integer scene;
    public final List<GltfScene> scenes = new ArrayList<>();
    public final List<GltfNode> nodes = new ArrayList<>();
    public final List<GltfMesh> meshes = new ArrayList<>();
    public final List<GltfAccessor> accessors = new ArrayList<>();
    public final List<GltfBufferView> bufferViews = new ArrayList<>();
    public final List<GltfBuffer> buffers = new ArrayList<>();
    public final List<GltfSkin> skins = new ArrayList<>();

    public void setScene(GltfScene scene) {
        if (!scenes.contains(scene)) {
            scenes.add(scene);
        }
        this.scene = scenes.indexOf(scene);
    }

    @Nullable
    public GltfNode findNodeByName(@NotNull String name) {
        for (GltfNode node : nodes) {
            if (node.name.equals(name)) {
                return node;
            }
        }
        return null;
    }

}
