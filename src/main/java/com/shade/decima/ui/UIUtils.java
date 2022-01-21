package com.shade.decima.ui;

import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.util.NotNull;

import javax.swing.tree.TreePath;
import java.util.ArrayDeque;
import java.util.Deque;

public final class UIUtils {
    private UIUtils() {
    }

    @NotNull
    public static TreePath getPath(@NotNull NavigatorNode node) {
        final Deque<NavigatorNode> nodes = new ArrayDeque<>();
        for (NavigatorNode current = node; current != null; current = current.getParent()) {
            nodes.offerFirst(current);
        }
        return new TreePath(nodes.toArray());
    }
}
