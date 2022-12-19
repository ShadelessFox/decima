package com.shade.decima.ui.data.viewer.model.gltf;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GltfSkin {
    public String name;
    public Integer inverseBindMatrices;
    public Integer skeleton;
    public List<Integer> joints = new ArrayList<>();

    public GltfSkin(@NotNull String name) {
        this.name = name;
    }

}
