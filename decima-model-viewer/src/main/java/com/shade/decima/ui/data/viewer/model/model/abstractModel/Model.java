package com.shade.decima.ui.data.viewer.model.model.abstractModel;

import com.shade.decima.ui.data.viewer.model.model.utils.Matrix4x4;

import java.util.ArrayList;
import java.util.List;

public class Model {
    public String name;
    public Skeleton skeleton = null;
    public Matrix4x4 matrix;
    public List<Mesh> meshes = new ArrayList<>();


}
