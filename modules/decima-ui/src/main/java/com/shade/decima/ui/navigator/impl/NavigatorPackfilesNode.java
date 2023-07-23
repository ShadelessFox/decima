package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.ui.navigator.NavigatorPath;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.Arrays;

public class NavigatorPackfilesNode extends NavigatorNode {
    private final String name;
    private final Packfile[] packfiles;

    public NavigatorPackfilesNode(@Nullable NavigatorNode parent, @NotNull String name, @NotNull Packfile[] packfiles) {
        super(parent);
        this.name = name;
        this.packfiles = packfiles;
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        return Arrays.stream(packfiles)
            .map(project -> new NavigatorPackfileNode(this, project))
            .toArray(NavigatorPackfileNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return name;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("Node.archiveIcon");
    }

    @Override
    public boolean contains(@NotNull NavigatorPath path) {
        for (Packfile packfile : packfiles) {
            if (packfile.getPath().getFileName().toString().equals(path.packfileId())) {
                return true;
            }
        }

        return false;
    }
}
