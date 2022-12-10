package com.shade.decima.ui.data.viewer.model.dmf;

public class DMFVertexAttribute {
    public String semantic;
    public int elementCount;
    public String elementType;
    public int size;
    public Integer stride;
    public Integer offset;
    public int bufferViewId;

    public void setBufferView(DMFBufferView bufferView, DMFSceneFile scene) {
        if (!scene.bufferViews.contains(bufferView)) {
            scene.bufferViews.add(bufferView);
        }
        bufferViewId = scene.bufferViews.indexOf(bufferView);
    }
}
