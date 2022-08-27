package com.shade.decima.ui.data.viewer.texture.controls;

import com.shade.util.NotNull;

import java.awt.*;

public interface ImageProvider {
    @NotNull
    Image getImage(int mip, int slice);

    int getMaxWidth();

    int getMaxHeight();

    int getMipCount();

    int getSliceCount();
}
