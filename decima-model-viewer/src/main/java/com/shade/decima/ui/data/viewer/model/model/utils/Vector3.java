package com.shade.decima.ui.data.viewer.model.model.utils;

import com.shade.util.NotNull;

public record Vector3(double x, double y, double z) {
    public Vector3(@NotNull double[] xyz) {
        this(xyz[0], xyz[1], xyz[2]);
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    @NotNull
    public Vector3 normalized() {
        final double norm = magnitude();
        return new Vector3(x / norm, y / norm, z / norm);
    }

    @NotNull
    public double[] toArray() {
        return new double[]{x(), y(), z()};
    }
}
