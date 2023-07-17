package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.util.FilePath;
import com.shade.decima.ui.editor.NodeEditorInputSimple;
import com.shade.decima.ui.navigator.NavigatorPath;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.editors.EditorManager;
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
        this.extension = IOUtils.getExtension(path.last());
    }

    @NotNull
    @Override
    public String getLabel() {
        return path.last();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return switch (extension) {
            case "core" -> UIManager.getIcon("Editor.coreIcon");
            case "stream" -> UIManager.getIcon("Editor.binaryIcon");
            default -> super.getIcon();
        };
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

    @Override
    public boolean contains(@NotNull NavigatorPath path) {
        return this.path.equals(path.filePath());
    }

    @NotNull
    public FilePath getPath() {
        return path;
    }

    @NotNull
    public String getExtension() {
        return extension;
    }

    public long getHash() {
        return path.hash();
    }

    public int getSize() {
        return size;
    }

    @Override
    public void actionPerformed(@NotNull InputEvent event) {
        EditorManager.getInstance().openEditor(new NodeEditorInputSimple(this), !event.isControlDown());
        event.consume();
    }
}
