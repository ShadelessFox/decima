package com.shade.decima.model.util.graph;

import com.shade.util.NotNull;

import java.awt.*;

public interface GraphLayoutConfig<V> {
    @NotNull
    Dimension getSize(@NotNull V vertex);

    @NotNull
    Dimension getSpacing();
}
