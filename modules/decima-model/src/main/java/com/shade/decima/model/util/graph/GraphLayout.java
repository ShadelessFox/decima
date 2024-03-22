package com.shade.decima.model.util.graph;

import com.shade.util.NotNull;

import java.awt.*;

public interface GraphLayout<V> {
    @NotNull
    V getVertex();

    @NotNull
    Point getLocation();

    @NotNull
    Dimension getSize();
}
