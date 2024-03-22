package com.shade.decima.model.util.graph.impl;

import com.shade.decima.model.util.graph.Graph;
import com.shade.util.NotNull;

import java.util.*;

public class DirectedAcyclicGraph<V> implements Graph<V> {
    private final Map<V, EdgeContainer<V>> vertices = new IdentityHashMap<>();

    @Override
    public void addVertex(@NotNull V vertex) {
        vertices.computeIfAbsent(vertex, v -> new EdgeContainer<>());
    }

    @Override
    public void addEdge(@NotNull V source, @NotNull V target) {
        checkVertex(source);
        checkVertex(target);

        if (source.equals(target)) {
            throw new IllegalArgumentException("Can't add edge to itself");
        }

        // Will throw an exception if a cycle is detected
        dfsF(target, new HashSet<>(), new HashSet<>());

        vertices.get(source).outgoing.add(target);
        vertices.get(target).incoming.add(source);
    }

    @Override
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
    @Override
    public Set<V> vertexSet() {
        return vertices.keySet();
    }

    @NotNull
    @Override
    public Set<V> incomingVerticesOf(@NotNull V vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableSet(vertices.get(vertex).incoming);
    }

    @NotNull
    @Override
    public Set<V> outgoingVerticesOf(@NotNull V vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableSet(vertices.get(vertex).outgoing);
    }

    private void checkVertex(@NotNull V vertex) {
        if (!vertices.containsKey(vertex)) {
            throw new IllegalArgumentException("Vertex is not present in the graph: " + vertex);
        }
    }

    private void dfsF(@NotNull V u, @NotNull Set<V> discovered, Set<V> finished) {
        discovered.add(u);

        for (V v : outgoingVerticesOf(u)) {
            if (discovered.contains(v)) {
                throw new IllegalArgumentException("Cycle detected: found a back edge from " + u + " to " + v);
            }

            if (finished.contains(v)) {
                continue;
            }

            dfsF(v, discovered, finished);
        }

        discovered.remove(u);
        finished.add(u);
    }

    private static class EdgeContainer<V> {
        private final Set<V> incoming = new HashSet<>();
        private final Set<V> outgoing = new HashSet<>();
    }
}
