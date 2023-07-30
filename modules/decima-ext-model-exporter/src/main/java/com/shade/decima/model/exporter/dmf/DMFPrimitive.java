package com.shade.decima.model.exporter.dmf;

import com.shade.util.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DMFPrimitive {
    public final int groupingId;
    public final int vertexCount;
    public final int vertexStart;
    public final int vertexEnd;

    public final int indexCount;
    public final int indexStart;
    public final int indexEnd;
    public final int indexSize;
    public int indexBufferViewId;

    public Integer materialId;

    public final Map<String, DMFVertexAttribute> vertexAttributes = new HashMap<>();
    public final DMFVertexBufferType vertexType;

    public DMFPrimitive(int groupingId, int vertexCount, @NotNull DMFVertexBufferType bufferType, int vertexStart, int vertexEnd, int indexSize, int indexCount, int indexStart, int indexEnd) {
        this.groupingId = groupingId;
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
