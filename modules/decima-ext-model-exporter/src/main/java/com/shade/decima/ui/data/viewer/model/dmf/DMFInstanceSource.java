package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.decima.ui.data.viewer.model.dmf.nodes.DMFNode;
import com.shade.util.NotNull;

public record DMFInstanceSource(@NotNull String uuid, @NotNull DMFNode rootNode) {
}
