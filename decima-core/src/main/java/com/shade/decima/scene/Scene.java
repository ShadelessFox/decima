package com.shade.decima.scene;

import java.util.List;

public record Scene(List<Node> nodes) {
    public Scene {
        nodes = List.copyOf(nodes);
    }

    public static Scene of(Node node) {
        return new Scene(List.of(node));
    }
}
