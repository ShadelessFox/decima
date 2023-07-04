package com.shade.decima.model.util;

import com.shade.util.NotNull;

import java.util.*;

public class Graph<V> {
    private final Map<V, EdgeContainer<V>> vertices = new LinkedHashMap<>();

    public void addVertex(@NotNull V vertex) {
        vertices.computeIfAbsent(vertex, v -> new EdgeContainer<>());
    }

    public void addEdge(@NotNull V source, @NotNull V target) {
        checkVertex(source);
        checkVertex(target);

        if (source.equals(target)) {
            throw new IllegalArgumentException("Can't add edge to itself");
        }

        vertices.get(source).outgoing.add(target);
        vertices.get(target).incoming.add(source);
    }

    public void removeVertex(@NotNull V vertex) {
        checkVertex(vertex);

        final EdgeContainer<V> container = vertices.remove(vertex);

        for (V incoming : container.incoming) {
            vertices.get(incoming).outgoing.remove(vertex);
        }

        for (V outgoing : container.outgoing) {
            vertices.get(outgoing).incoming.remove(vertex);
        }
    }

    @NotNull
    public Set<V> vertexSet() {
        return vertices.keySet();
    }

    @NotNull
    public Set<V> incomingVerticesOf(@NotNull V vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableSet(vertices.get(vertex).incoming);
    }

    @NotNull
    public Set<V> outgoingVerticesOf(@NotNull V vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableSet(vertices.get(vertex).outgoing);
    }

    private void checkVertex(@NotNull V vertex) {
        if (!vertices.containsKey(vertex)) {
            throw new IllegalArgumentException("Vertex is not present in the graph: " + vertex);
        }
    }

    private static class EdgeContainer<V> {
        private final Set<V> incoming = new HashSet<>();
        private final Set<V> outgoing = new HashSet<>();
    }
}
