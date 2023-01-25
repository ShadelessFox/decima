package com.shade.decima.ui.data.viewer.model.model.dmf;

import java.util.HashMap;
import java.util.Map;

public class DMFPrimitive {
    public int groupingId;
    public int vertexCount;
    public int vertexStart;
    public int vertexEnd;

    public int indexCount;
    public int indexStart;
    public int indexEnd;
    public int indexSize;
    public int indexBufferViewId;

    public Integer materialId;

    public Map<String, DMFVertexAttribute> vertexAttributes = new HashMap<>();
    public DMFVertexBufferType vertexType;


    public DMFPrimitive(int vertexCount, DMFVertexBufferType bufferType, int vertexStart, int vertexEnd, int indexSize, int indexCount, int indexStart, int indexEnd) {
        this.vertexCount = vertexCount;
        this.vertexType = bufferType;
        this.vertexStart = vertexStart;
        this.vertexEnd = vertexEnd;
        this.indexSize = indexSize;
        this.indexCount = indexCount;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;

    }

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
