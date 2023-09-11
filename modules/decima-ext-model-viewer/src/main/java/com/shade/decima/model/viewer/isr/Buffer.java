package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;

public record Buffer(@NotNull byte[] data) {
    public int length() {
        return data.length;
    }
}
