package com.shade.decima.ui.data.viewer.model.abstractModel;


import com.shade.decima.ui.data.viewer.model.utils.Matrix4x4;

public class Bone {

    public String name;
    public Matrix4x4 transform;
    public Matrix4x4 inverseBindMatrix = null;
    public boolean isRelative = false;
    public int parent;

    public Bone(String name, Matrix4x4 transform, int parent) {
        this.name = name;
        this.transform = transform;
        this.parent = parent;
    }

    public Bone(String name, Matrix4x4 transform) {
        this.name = name;
        this.transform = transform;
    }

    public Bone(String name) {
        this.name = name;
    }
}
