package com.shade.decima.ui.data.viewer.mesh;

import com.shade.decima.ui.data.viewer.mesh.gltf.GltfFile;
import com.shade.decima.ui.data.viewer.mesh.gltf.GltfSkin;
import com.shade.decima.ui.data.viewer.mesh.utils.Matrix4x4;

import java.util.ArrayList;
import java.util.List;

public class ModelExportContext {
    GltfFile file;
    String resourceName;
    GltfSkin currentSkin;
    List<Matrix4x4> parentMatrices = new ArrayList<>();
    List<Matrix4x4> localMatrices = new ArrayList<>();
    List<String> boneNames = new ArrayList<>();

    public ModelExportContext(String resourceName) {
        this.resourceName = resourceName;
        file = new GltfFile();
    }
}
