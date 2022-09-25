package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.base.GameType;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;

public class CoreNodeBinary extends TreeNodeLazy {
    private final CoreBinary binary;
    private final ProjectContainer project;

    public CoreNodeBinary(@NotNull CoreBinary binary, @NotNull ProjectContainer project) {
        super(null);
        this.binary = binary;
        this.project = project;
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        return binary.entries().stream()
            .map(entry -> new CoreNodeEntry(this, entry))
            .toArray(TreeNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return "<root>";
    }

    @NotNull
    public GameType getGameType() {
        return project.getType();
    }
}
