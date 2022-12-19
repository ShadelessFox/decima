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


    public GltfScene currentScene() {
        if (scene != null) {
            return scenes.get(scene);
        } else if (!scenes.isEmpty()) {
            return scenes.get(0);
        }
        return newScene();
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
    public GltfNode getNodeByName(@NotNull String name) {
        for (GltfNode node : nodes) {
            if (node.name.equals(name)) {
                return node;
            }
        }
        return null;
//        throw new IllegalStateException("Node %s not found".formatted(name));
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
        return node;
    }

    public GltfNode newNode(String name, @Nullable GltfNode parent) {
        GltfNode node = new GltfNode(name);
        nodes.add(node);
        if (parent != null)
            parent.children.add(nodes.indexOf(node));
        return node;
    }

    public GltfNode newNode(String name, GltfMesh mesh) {
        if (!meshes.contains(mesh)) {
            meshes.add(mesh);
        }
        GltfNode node = new GltfNode(name, meshes.indexOf(mesh));
        nodes.add(node);
        return node;
    }

    public void addChild(GltfNode parent, GltfNode child) {
        parent.children.add(nodes.indexOf(child));
    }

    public void addToScene(GltfScene scene, GltfNode node) {
        scene.addNode(nodes.indexOf(node));
    }

    public void addToScene(GltfNode node) {
        currentScene().addNode(nodes.indexOf(node));
    }

    public GltfSkin newSkin(String name) {
        GltfSkin skin = new GltfSkin(name);
        skins.add(skin);
        return skin;
    }

    public short addBone(GltfSkin skin, GltfNode node) {
        int boneId = skin.joints.size();
        skin.joints.add(nodes.indexOf(node));
        return (short) boneId;
    }
}
