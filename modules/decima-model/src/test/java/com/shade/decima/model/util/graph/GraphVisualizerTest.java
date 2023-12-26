package com.shade.decima.model.util.graph;

import com.shade.decima.model.util.graph.impl.DefaultGraphLayout;
import com.shade.decima.model.util.graph.impl.DirectedAcyclicGraph;
import com.shade.decima.model.util.graph.impl.HorizontalGraphVisualizer;
import com.shade.util.NotNull;
import org.junit.jupiter.api.Assertions;

import java.awt.*;
import java.util.List;

public class GraphVisualizerTest {
    public static void main(String[] args) {
        final var graph = new DirectedAcyclicGraph<>();
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
        graph.addVertex("D");
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "D");

        final var visualizer = new HorizontalGraphVisualizer<>();
        final var config = new GraphLayoutConfig<>() {
            @NotNull
            @Override
            public Dimension getSize(@NotNull Object vertex) {
                return new Dimension(20, 10);
            }

            @NotNull
            @Override
            public Dimension getSpacing() {
                return new Dimension(5, 5);
            }
        };

        final var layouts = visualizer.create(graph, config);
        final var expected = List.of(
            new DefaultGraphLayout<>("A", new Point(0, 0), new Dimension(20, 10)),
            new DefaultGraphLayout<>("B", new Point(25, 0), new Dimension(20, 10)),
            new DefaultGraphLayout<>("C", new Point(25, 15), new Dimension(20, 10)),
            new DefaultGraphLayout<>("D", new Point(50, 0), new Dimension(20, 10))
        );

        Assertions.assertEquals(expected, layouts);
    }
}
