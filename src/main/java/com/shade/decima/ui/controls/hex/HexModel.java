package com.shade.decima.ui.controls.hex;

import com.shade.util.NotNull;

public interface HexModel {
    byte get(int index);

    void get(int index, @NotNull byte[] dst, int offset, int length);

    int length();
}
