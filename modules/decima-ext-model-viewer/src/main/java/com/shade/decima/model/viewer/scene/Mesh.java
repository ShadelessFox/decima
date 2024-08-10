package com.shade.decima.model.viewer.scene;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public record Mesh(@NotNull List<Primitive> primitives) {
    public Mesh() {
        this(new ArrayList<>());
    }

    public void add(@NotNull Primitive primitive) {
        primitives.add(primitive);
    }
}
