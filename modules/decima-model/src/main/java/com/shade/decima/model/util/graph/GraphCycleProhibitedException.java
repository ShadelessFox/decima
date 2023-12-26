package com.shade.decima.model.util.graph;

import com.shade.util.NotNull;

public class GraphCycleProhibitedException extends RuntimeException {
    public GraphCycleProhibitedException(@NotNull String message) {
        super(message);
    }
}
