package com.shade.decima.ui.bookmarks.impl;

import com.shade.decima.ui.bookmarks.BookmarkManager;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;

import java.util.Arrays;

public class BookmarkNodeRoot extends TreeNodeLazy {
    public BookmarkNodeRoot() {
        super(null);
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Bookmarks";
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        return Arrays.stream(BookmarkManager.getInstance().getBookmarks())
            .map(bookmark -> new BookmarkNode(this, bookmark))
            .toArray(TreeNode[]::new);
    }
}
