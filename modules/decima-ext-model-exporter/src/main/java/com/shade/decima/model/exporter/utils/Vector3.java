package com.shade.decima.model.exporter.utils;

import com.shade.util.NotNull;

public record Vector3(double x, double y, double z) {
    public Vector3(@NotNull double[] xyz) {
        this(xyz[0], xyz[1], xyz[2]);
    }

    @NotNull
    public double[] toArray() {
        return new double[]{x(), y(), z()};
    }
}
