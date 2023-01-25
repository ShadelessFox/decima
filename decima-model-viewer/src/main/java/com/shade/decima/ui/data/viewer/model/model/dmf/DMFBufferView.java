package com.shade.decima.ui.data.viewer.model.model.dmf;

public class DMFBufferView {
    public int bufferId;
    public int offset;
    public int size;

    public void setBuffer(DMFBuffer buffer, DMFSceneFile scene) {
        if (!scene.buffers.contains(buffer)) {
            scene.buffers.add(buffer);
        }
        bufferId = scene.buffers.indexOf(buffer);
    }
}
