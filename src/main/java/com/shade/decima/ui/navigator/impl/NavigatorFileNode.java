package com.shade.decima.ui.navigator.impl;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.FileEditorInputSimple;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.Optional;

public class NavigatorFileNode extends NavigatorNode implements TreeNode.ActionListener {
    private final FilePath path;
    private final int size;
    private final String extension;

    public NavigatorFileNode(@Nullable NavigatorNode parent, @NotNull FilePath path) {
        super(parent);
        this.path = path;
        this.size = Optional.ofNullable(getPackfile().getFileEntry(path.hash()))
            .map(entry -> entry.span().size())
            .orElse(-1);
        this.extension = path.last().substring(path.last().indexOf('.') + 1);
    }

    @NotNull
    @Override
    public String getLabel() {
        return path.last();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        if (extension.isEmpty() || extension.equals("core.stream")) {
            return UIManager.getIcon("Navigator.binaryIcon");
        } else {
            return super.getIcon();
        }
    }

    @Override
    protected boolean allowsChildren() {
        return false;
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        throw new IllegalStateException("Should not be called");
    }

    @NotNull
    public FilePath getPath() {
        return path;
    }

    public long getHash() {
        return path.hash();
    }

    public int getSize() {
        return size;
    }

    @Override
    public void actionPerformed(@NotNull InputEvent event) {
        Application.getFrame().getEditorManager().openEditor(new FileEditorInputSimple(this), !event.isControlDown());
        event.consume();
    }
}
