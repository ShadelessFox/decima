package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.packfile.Packfile;
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

public class NavigatorFileNode extends NavigatorNode implements TreeNode.ActionListener {
    private final ArchiveFile file;
    private final FilePath path;
    private final String extension;

    public NavigatorFileNode(@Nullable NavigatorNode parent, @NotNull ArchiveFile file, @NotNull FilePath path) {
        super(parent);
        this.file = file;
        this.path = path;
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

    @Override
    @NotNull
    public Packfile getPackfile() {
        return (Packfile) file.getArchive();
    }

    @NotNull
    public Archive getArchive() {
        return file.getArchive();
    }

    @NotNull
    public ArchiveFile getFile() {
        return file;
    }

    public long getHash() {
        return path.hash();
    }

    @Override
    public void actionPerformed(@NotNull InputEvent event) {
        EditorManager.getInstance().openEditor(new NodeEditorInputSimple(this), !event.isControlDown());
        event.consume();
    }
}
