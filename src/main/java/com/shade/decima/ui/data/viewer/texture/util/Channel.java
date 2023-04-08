package com.shade.decima.ui.data.viewer.texture.util;

import com.shade.util.NotNull;

import java.awt.*;

public enum Channel {
    R("Red", Color.RED) {
        @Override
        public int getComponent(int rgb) {
            return rgb >> 16 & 0xff;
        }

        @Override
        public int setComponent(int rgb, int value) {
            return rgb & 0xff00ffff | value << 16;
        }
    },
    G("Green", Color.GREEN) {
        @Override
        public int getComponent(int rgb) {
            return rgb >> 8 & 0xff;
        }

        @Override
        public int setComponent(int rgb, int value) {
            return rgb & 0xffff00ff | value << 8;
        }
    },
    B("Blue", Color.BLUE) {
        @Override
        public int getComponent(int rgb) {
            return rgb & 0xff;
        }

        @Override
        public int setComponent(int rgb, int value) {
            return rgb & 0xffffff00 | value;
        }
    },
    A("Alpha", Color.BLACK) {
        @Override
        public int getComponent(int rgb) {
            return rgb >> 24 & 0xff;
        }

        @Override
        public int setComponent(int rgb, int value) {
            return rgb & 0x00ffffff | value << 24;
        }
    };

    private final String name;
    private final Color color;

    Channel(@NotNull String name, @NotNull Color color) {
        this.name = name;
        this.color = color;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Color getColor() {
        return color;
    }

    public abstract int getComponent(int rgb);

    public abstract int setComponent(int rgb, int value);
}
