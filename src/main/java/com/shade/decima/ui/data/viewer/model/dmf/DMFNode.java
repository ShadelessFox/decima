package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DMFNode {
    public DMFNodeType type = DMFNodeType.NODE;
    public String name;

    public final List<DMFNode> children = new ArrayList<>();
    public List<Integer> collectionIds = new ArrayList<>();
    public DMFTransform transform;

    public DMFNode() {
    }

    public DMFNode(@NotNull String name) {
        this();
        this.name = name;
    }

    public void addToCollection(@NotNull DMFCollection collection, @NotNull DMFSceneFile scene) {
        collectionIds.add(scene.collections.indexOf(collection));
    }
}
