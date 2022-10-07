package com.shade.decima.ui.data.viewer.mesh.dmf;

import java.util.HashMap;
import java.util.Map;

public class DMFPrimitive {
    public int groupingId;
    public int vertexCount;
    public int vertexStart;
    public int vertexEnd;
    public Map<String, DMFVertexAttribute> vertexAttributes = new HashMap<>();
    public DMFVertexType vertexType = DMFVertexType.MULTIBUFFER;

    public int indexCount;
    public int indexStart;
    public int indexEnd;
    public int indexSize;
    public int indexOffset;
    public int indexBufferViewId;


    public Integer materialId;

    public void setIndexBufferView(DMFBufferView indexBufferView, DMFSceneFile scene) {
        if (!scene.bufferViews.contains(indexBufferView)) {
            scene.bufferViews.add(indexBufferView);
        }
        indexBufferViewId = scene.bufferViews.indexOf(indexBufferView);
    }

    public void setMaterial(DMFMaterial material, DMFSceneFile scene) {
        if (!scene.materials.contains(material)) {
            scene.materials.add(material);
        }
        materialId = scene.materials.indexOf(material);
    }
}
