package com.shade.decima.app.ui.tree;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.tree.TreePath;
import java.awt.event.InputEvent;
import java.util.EventObject;

public class TreeActionEvent extends EventObject {
    private final TreePath path;
    private final int row;

    public TreeActionEvent(@NotNull InputEvent source, @NotNull TreePath path, int row) {
        super(source);
        this.path = path;
        this.row = row;
    }

    @Override
    public InputEvent getSource() {
        return (InputEvent) super.getSource();
    }

    @NotNull
    public TreePath getPath() {
        return path;
    }

    public int getRow() {
        return row;
    }

    @Nullable
    public Object getLastPathComponent() {
        return path.getLastPathComponent();
    }
}
