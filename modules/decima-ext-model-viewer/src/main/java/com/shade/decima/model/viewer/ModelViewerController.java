package com.shade.decima.model.viewer;

import com.shade.decima.model.viewer.isr.Node;
import com.shade.util.NotNull;

import java.util.Set;

public interface ModelViewerController {
    void setSelection(@NotNull Set<Node> nodes);

    boolean isSelected(@NotNull Node node);
}
