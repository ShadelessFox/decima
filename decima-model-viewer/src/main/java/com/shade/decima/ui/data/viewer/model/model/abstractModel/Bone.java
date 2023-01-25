package com.shade.decima.ui.data.viewer.model.model.abstractModel;


import com.shade.decima.ui.data.viewer.model.model.utils.Matrix4x4;

public class Bone {

    public String name;
    public Matrix4x4 matrix;
    public Matrix4x4 inverseBindMatrix = null;
    public boolean isRelative = false;
    public int parent;

    public Bone(String name, Matrix4x4 matrix, int parent) {
        this.name = name;
        this.matrix = matrix;
        this.parent = parent;
    }

    public Bone(String name, Matrix4x4 matrix) {
        this.name = name;
        this.matrix = matrix;
    }

    public Bone(String name) {
        this.name = name;
    }
}
