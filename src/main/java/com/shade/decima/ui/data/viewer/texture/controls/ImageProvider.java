package com.shade.decima.ui.data.viewer.texture.controls;

import com.shade.util.NotNull;

import java.awt.image.BufferedImage;

public interface ImageProvider {
    @NotNull
    BufferedImage getImage(int mip, int slice);

    int getMaxWidth();

    int getMaxHeight();

    int getMipCount();

    int getSliceCount(int mip);
}
