package com.shade.decima.ui.data.viewer.mesh.dmf;

import java.util.HashMap;
import java.util.Map;

public class DMFPrimitive {
    public int vertexCount;
    public int vertexStart;
    public int vertexEnd;
    public Map<String, DMFVertexAttribute> vertexAttributes = new HashMap<>();
    public DMFVertexType vertexType = DMFVertexType.SPARSE;

    public int indexCount;
    public int indexStart;
    public int indexEnd;
    public int indexSize;
    public int indexBufferId;


    public Integer materialId;

    public void setIndexBuffer(DMFBuffer indexBuffer, DMFSceneFile scene) {
        scene.buffers.add(indexBuffer);
        indexBufferId = scene.buffers.indexOf(indexBuffer);
    }
}
