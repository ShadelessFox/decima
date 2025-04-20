package com.shade.decima.ui.bookmarks.impl;

import com.shade.decima.ui.bookmarks.Bookmark;
import com.shade.decima.ui.editor.NodeEditorInputLazy;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.awt.event.InputEvent;
import java.util.StringJoiner;

public class BookmarkNode extends TreeNode implements TreeNode.ActionListener {
    private final Bookmark bookmark;

    public BookmarkNode(@NotNull BookmarkNodeRoot parent, @NotNull Bookmark bookmark) {
        super(parent);
        this.bookmark = bookmark;
    }

    @NotNull
    @Override
    public String getLabel() {
        return bookmark.name();
    }

    @Nullable
    @Override
    public String getDescription() {
        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Project: " + bookmark.location().project());
        joiner.add("Packfile: " + bookmark.location().packfile());
        joiner.add("Path: " + bookmark.location().path());
        return joiner.toString();
    }

    @Override
    protected boolean allowsChildren() {
        return false;
    }

    @NotNull
    public Bookmark getBookmark() {
        return bookmark;
    }

    @Override
    public void actionPerformed(@NotNull InputEvent event) {
        final Bookmark.Location location = bookmark.location();
        final NodeEditorInputLazy input = new NodeEditorInputLazy(location.project(), location.packfile(), location.path());

        EditorManager.getInstance().openEditor(input, !event.isControlDown());
        event.consume();
    }
}
