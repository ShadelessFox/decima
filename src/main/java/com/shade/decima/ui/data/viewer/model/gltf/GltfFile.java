package com.shade.decima.ui.data.viewer.model.gltf;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GltfFile {
    public GltfAsset asset;
    public Integer scene;
    public final List<GltfScene> scenes = new ArrayList<>();
    public final List<GltfNode> nodes = new ArrayList<>();
    public final List<GltfMesh> meshes = new ArrayList<>();
    public final List<GltfSkin> skins = new ArrayList<>();
    public final List<GltfAccessor> accessors = new ArrayList<>();
    public final List<GltfBufferView> bufferViews = new ArrayList<>();
    public final List<GltfBuffer> buffers = new ArrayList<>();

    @Nullable
    private GltfScene currentScene() {
        if (scene != null) {
            return scenes.get(scene);
        } else if (!scenes.isEmpty()) {
            return scenes.get(0);
        }
        return null;
    }

    public void setScene(GltfScene scene) {
        if (!scenes.contains(scene)) {
            scenes.add(scene);
        }
        this.scene = scenes.indexOf(scene);
    }

    public GltfScene newScene() {
        GltfScene scene = new GltfScene();
        scenes.add(scene);
        return scene;
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

    public GltfNode newNode() {
        GltfNode node = new GltfNode();
        nodes.add(node);
        Objects.requireNonNull(currentScene()).addNode(nodes.indexOf(node));
        return node;
    }

    public GltfNode newNode(String name) {
        GltfNode node = new GltfNode(name);
        nodes.add(node);
        Objects.requireNonNull(currentScene()).addNode(nodes.indexOf(node));
        return node;
    }

    public GltfNode newNode(String name, GltfMesh mesh) {
        if (!meshes.contains(mesh)) {
            meshes.add(mesh);
        }
        GltfNode node = new GltfNode(name, meshes.indexOf(mesh));
        nodes.add(node);
        Objects.requireNonNull(currentScene()).addNode(nodes.indexOf(node));
        return node;
    }
}
