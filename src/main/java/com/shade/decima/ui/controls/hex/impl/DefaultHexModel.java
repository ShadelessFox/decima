package com.shade.decima.ui.controls.hex.impl;

import com.shade.decima.ui.controls.hex.HexModel;
import com.shade.util.NotNull;

public record DefaultHexModel(@NotNull byte[] data) implements HexModel {
    @Override
    public byte get(int index) {
        return data[index];
    }

    @Override
    public void get(int index, @NotNull byte[] dst, int offset, int length) {
        System.arraycopy(data, index, dst, offset, length);
    }

    @Override
    public int length() {
        return data.length;
    }
}
