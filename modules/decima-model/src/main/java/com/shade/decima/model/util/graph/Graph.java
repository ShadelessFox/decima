package com.shade.decima.model.util.graph;

import com.shade.util.NotNull;

import java.util.Set;

public interface Graph<V> {
    void addVertex(@NotNull V vertex);

    void addEdge(@NotNull V source, @NotNull V target);

    void removeVertex(@NotNull V vertex);

    @NotNull
    Set<V> vertexSet();

    @NotNull
    Set<V> incomingVerticesOf(@NotNull V vertex);

    @NotNull
    Set<V> outgoingVerticesOf(@NotNull V vertex);
}
