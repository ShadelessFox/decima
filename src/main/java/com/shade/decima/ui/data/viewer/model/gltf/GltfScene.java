package com.shade.decima.ui.data.viewer.model.gltf;

import java.util.ArrayList;
import java.util.List;

public class GltfScene {
    public final List<Integer> nodes = new ArrayList<>();

    GltfScene() {
    }


    public void addNode(GltfNode node, GltfFile file) {
        nodes.add(file.nodes.indexOf(node));
    }
}
