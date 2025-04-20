package com.shade.decima.model.util.graph.impl;

import com.shade.decima.model.util.graph.Graph;
import com.shade.decima.model.util.graph.GraphLayout;
import com.shade.decima.model.util.graph.GraphLayoutConfig;
import com.shade.decima.model.util.graph.GraphVisualizer;
import com.shade.util.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class HorizontalGraphVisualizer<V> implements GraphVisualizer<V, GraphLayoutConfig<? super V>> {
    @NotNull
    @Override
    public Collection<? extends GraphLayout<V>> create(@NotNull Graph<V> graph, @NotNull GraphLayoutConfig<? super V> config) {
        final var roots = getRoots(graph);
        final var levels = collectLevels(graph, roots);
        final var layouts = new ArrayList<GraphLayout<V>>(graph.vertexSet().size());
        final var spacing = config.getSpacing();

        int x = 0;

        for (List<V> level : levels) {
            int width = 0;
            int y = 0;

            for (V vertex : level) {
                final Dimension size = config.getSize(vertex);

                layouts.add(new DefaultGraphLayout<>(vertex, new Point(x, y), size));

                width = Math.max(width, size.width);
                y += size.height + spacing.height;
            }

            x += width + spacing.width;
        }

        return layouts;
    }

    @NotNull
    private static <V> List<List<V>> collectLevels(@NotNull Graph<V> graph, @NotNull List<V> roots) {
        return collectLevels(graph, roots, List.of(roots));
    }

    @NotNull
    private static <V> List<List<V>> collectLevels(@NotNull Graph<V> graph, @NotNull List<V> stableCurrentLevelNodes, @NotNull List<List<V>> currentTotalLevels) {
        if (stableCurrentLevelNodes.isEmpty()) {
            return currentTotalLevels;
        }

        final List<V> nextLevelNodes = getNextLevel(graph, stableCurrentLevelNodes);

        if (nextLevelNodes.isEmpty()) {
            return currentTotalLevels;
        }

        final List<WeightedVertex<V>> weightedCurrentLevelNodes = IntStream.range(0, stableCurrentLevelNodes.size())
            .mapToObj(i -> new WeightedVertex<>(stableCurrentLevelNodes.get(i), i))
            .toList();

        final List<V> weightedNextLevelNodes = nextLevelNodes.stream()
            .map(key -> new WeightedVertex<>(key, calculateChildrenWeight(graph, key, weightedCurrentLevelNodes)))
            .sorted(Comparator.comparingDouble(WeightedVertex::weight))
            .map(WeightedVertex::vertex)
            .toList();

        final List<List<V>> totalLevels = new ArrayList<>(currentTotalLevels);
        totalLevels.add(weightedNextLevelNodes);

        return collectLevels(graph, weightedNextLevelNodes, totalLevels);
    }

    @NotNull
    private static <V> List<V> getRoots(@NotNull Graph<V> graph) {
        return graph.vertexSet().stream()
            .filter(key -> graph.incomingVerticesOf(key).isEmpty())
            .toList();
    }

    @NotNull
    private static <V> List<V> getNextLevel(@NotNull Graph<V> graph, @NotNull List<V> roots) {
        return roots.stream()
            .flatMap(key -> graph.outgoingVerticesOf(key).stream())
            .distinct()
            .toList();
    }

    private static <V> double calculateChildrenWeight(@NotNull Graph<V> graph, @NotNull V vertex, @NotNull List<WeightedVertex<V>> weightedParentNodes) {
        return weightedParentNodes.stream()
            .filter(key -> graph.outgoingVerticesOf(key.vertex()).contains(vertex))
            .mapToDouble(WeightedVertex::weight)
            .average().orElse(0.0);
    }

    private record WeightedVertex<V>(V vertex, double weight) {}
}
