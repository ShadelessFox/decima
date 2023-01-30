package com.shade.decima.model.rtti.types.java;

import com.shade.util.NotNull;

public interface HwTextureHeader extends HwType {
    @NotNull
    String getType();

    int getWidth();

    int getHeight();

    int getDepth();

    int getMipCount();

    @NotNull
    String getPixelFormat();
}
