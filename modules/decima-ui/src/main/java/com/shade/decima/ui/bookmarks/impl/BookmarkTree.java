package com.shade.decima.ui.bookmarks.impl;

import com.shade.platform.ui.controls.tree.Tree;

public class BookmarkTree extends Tree {
    public BookmarkTree() {
        setCellRenderer(new BookmarkTreeCellRenderer());
        setShowsRootHandles(false);
        setRootVisible(false);

        getModel().setRoot(new BookmarkNodeRoot());
    }
}
