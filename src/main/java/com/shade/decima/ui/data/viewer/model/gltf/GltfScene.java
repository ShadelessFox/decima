package com.shade.decima.ui.data.viewer.model.gltf;

import java.util.ArrayList;
import java.util.List;

public class GltfScene {
    public final List<Integer> nodes = new ArrayList<>();

    GltfScene() {
    }


    public void addNode(int nodeIndex) {
        nodes.add(nodeIndex);
    }
}
