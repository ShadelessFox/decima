package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DMFNode {
    public final DMFNodeType type;
    public final String name;

    public final List<DMFNode> children = new ArrayList<>();
    public List<Integer> collectionIds = new ArrayList<>();
    public DMFTransform transform = null;

    public DMFNode(@NotNull String name) {
        this(name, DMFNodeType.NODE);
    }

    protected DMFNode(@NotNull String name, @NotNull DMFNodeType type) {
        this.name = name;
        this.type = type;
    }

    public void addToCollection(@NotNull DMFCollection collection, @NotNull DMFSceneFile scene) {
        collectionIds.add(scene.collections.indexOf(collection));
    }
}
