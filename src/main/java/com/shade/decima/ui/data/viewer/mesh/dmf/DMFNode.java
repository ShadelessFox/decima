package com.shade.decima.ui.data.viewer.mesh.dmf;

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
    }
}
