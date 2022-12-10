package com.shade.decima.ui.data.viewer.model.gltf;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GltfMesh {
    public String name;
    public final List<Primitive> primitives = new ArrayList<>();

    public GltfMesh(@NotNull GltfFile file) {
        file.meshes.add(this);
    }

    public static class Primitive {
        public final Map<String, Integer> attributes = new HashMap<>();
        public int indices;

        public Primitive(@NotNull GltfMesh mesh) {
            mesh.primitives.add(this);
        }

        public void addAttribute(@NotNull GltfFile file, @NotNull String attributeName, @NotNull GltfAccessor accessor) {
            int accessorId = file.accessors.indexOf(accessor);
            attributes.put(attributeName, accessorId);
        }

    }

}
