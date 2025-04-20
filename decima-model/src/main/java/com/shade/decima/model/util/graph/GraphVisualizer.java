package com.shade.decima.model.util.graph;

import com.shade.util.NotNull;

import java.util.Collection;

public interface GraphVisualizer<V, C extends GraphLayoutConfig<? super V>> {
    @NotNull
    Collection<? extends GraphLayout<V>> create(@NotNull Graph<V> graph, @NotNull C config);
}
