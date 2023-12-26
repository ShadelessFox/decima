package com.shade.decima.model.util.graph.impl;

import com.shade.decima.model.util.graph.GraphLayout;
import com.shade.util.NotNull;

import java.awt.*;

public record DefaultGraphLayout<V>(@NotNull V vertex, @NotNull Point location, @NotNull Dimension size) implements GraphLayout<V> {
    @NotNull
    @Override
    public V getVertex() {
        return vertex;
    }

    @NotNull
    @Override
    public Point getLocation() {
        return location;
    }

    @NotNull
    @Override
    public Dimension getSize() {
        return size;
    }
}
