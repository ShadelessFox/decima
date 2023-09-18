package com.shade.decima.ui.data.viewer.model.utils;

import com.shade.util.NotNull;

public record Quaternion(double x, double y, double z, double w) {
    public Quaternion(@NotNull double[] xyzw) {
        this(xyzw[0], xyzw[1], xyzw[2], xyzw[3]);
    }

    @NotNull
    public Quaternion add(@NotNull Quaternion other) {
        return new Quaternion(x() + other.x(), y() + other.y(), z() + other.y(), w() + other.w());
    }

    public double magnitude() {
        return Math.sqrt(x() * x() + y() * y() + z() * z() + w() * w());
    }

    @NotNull
    public Quaternion normalized() {
        final double norm = magnitude();
        return new Quaternion(x() / norm, y() / norm, z() / norm, w() / norm);
    }

    @NotNull
    public double[] toArray() {
        return new double[]{x, y, z, w};
    }
}
