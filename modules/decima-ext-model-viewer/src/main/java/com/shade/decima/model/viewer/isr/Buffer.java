package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;

public record Buffer(@NotNull byte[] data) {
    @NotNull
    public BufferView asView() {
        return new BufferView(this, 0, data.length);
    }

    public int length() {
        return data.length;
    }
}
