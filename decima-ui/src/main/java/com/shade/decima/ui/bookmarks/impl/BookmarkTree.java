package com.shade.decima.ui.bookmarks.impl;

import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.util.UIUtils;

import java.awt.*;

public class BookmarkTree extends Tree {
    public BookmarkTree() {
        setCellRenderer(new BookmarkTreeCellRenderer());
        setShowsRootHandles(false);
        setRootVisible(false);

        getModel().setRoot(new BookmarkNodeRoot());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getModel().isEmpty()) {
            UIUtils.setRenderingHints(g);
            UIUtils.drawCenteredString(g, "No bookmarks\nRight-click on a file to bookmark it", getWidth(), getHeight());
        }
    }
}
