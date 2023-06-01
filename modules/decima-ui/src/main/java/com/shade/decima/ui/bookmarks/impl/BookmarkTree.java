package com.shade.decima.ui.bookmarks.impl;

import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeModel;

public class BookmarkTree extends Tree {
    public BookmarkTree() {
        super(new BookmarkNodeRoot(), TreeModel::new);
        setCellRenderer(new BookmarkTreeCellRenderer());
        setShowsRootHandles(false);
        setRootVisible(false);
    }
}
