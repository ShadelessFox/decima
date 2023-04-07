package com.shade.decima.ui.data.viewer.texture.util;

import com.shade.util.NotNull;

public enum RGBChannel {
    RGBA("Color + Alpha"),
    RGB("Color"),
    R("Red"),
    G("Green"),
    B("Blue"),
    A("Alpha");

    private final String name;

    RGBChannel(@NotNull String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
