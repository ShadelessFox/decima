package com.shade.decima.scene;

import java.util.List;

public record Scene(List<Node> nodes) {
    public Scene {
        nodes = List.copyOf(nodes);
    }
}
