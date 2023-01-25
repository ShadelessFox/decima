package com.shade.decima.ui.data.viewer.model.model.dmf;

public class DMFBone {
    public String name;
    public DMFTransform transform;
    public int parentId;
    public boolean localSpace = false;

    public DMFBone(String name, DMFTransform transform, int parentId) {
        this.name = name;
        this.transform = transform;
        this.parentId = parentId;
    }
}
