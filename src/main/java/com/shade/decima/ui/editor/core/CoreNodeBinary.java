package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;

import java.util.List;

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
        final List<RTTIObject> entries = binary.entries();
        final CoreNodeEntry[] children = new CoreNodeEntry[entries.size()];

        for (int i = 0; i < entries.size(); i++) {
            children[i] = new CoreNodeEntry(this, entries.get(i), i);
        }

        return children;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "<root>";
    }

    @NotNull
    public CoreBinary getBinary() {
        return binary;
    }

    @NotNull
    public GameType getGameType() {
        return project.getType();
    }
}
