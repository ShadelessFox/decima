package com.shade.decima.ui.data.viewer.model.model.dmf;

import java.util.ArrayList;
import java.util.List;

public class DMFNode {
    public String type = "Node";
    public String name;

    public List<Integer> collectionIds;
    public DMFTransform transform;

    public final List<DMFNode> children;

    public DMFNode() {
        children = new ArrayList<>();
        collectionIds = new ArrayList<>();
    }

    public DMFNode(String name) {
        this();
        this.name = name;
    }


    public void addToCollection(DMFCollection collection, DMFSceneFile scene) {
        collectionIds.add(scene.collections.indexOf(collection));
    }
}
