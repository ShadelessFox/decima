package com.shade.decima.ui.data.viewer.mesh.dmf;

public class DMFVertexAttribute {
    public String semantic;
    public String dataType;
    public String componentType;
    public int size;
    public Integer stride;
    public Integer offset;
    public int bufferId;

    public void setBuffer(DMFBuffer elementBuffer, DMFSceneFile scene) {
        scene.buffers.add(elementBuffer);
        bufferId = scene.buffers.indexOf(elementBuffer);
    }
}
