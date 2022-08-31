package com.shade.decima.ui.data.viewer.mesh.gltf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GltfMesh {
    public final List<Primitive> primitives = new ArrayList<>();

    public static class Primitive {
        public final Map<String, Integer> attributes = new HashMap<>();
        public int indices;
    }
}
