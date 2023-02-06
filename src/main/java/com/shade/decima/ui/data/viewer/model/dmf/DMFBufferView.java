package com.shade.decima.ui.data.viewer.model.dmf;

public class DMFBufferView {
    public int bufferId;
    public int offset;
    public int size;

    public DMFBufferView(int bufferId, int offset, int size) {
        this.bufferId = bufferId;
        this.offset = offset;
        this.size = size;
    }
}
