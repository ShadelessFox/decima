package com.shade.decima.ui.navigator;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public abstract class NavigatorNode implements TreeNode {
    @Override
    public TreeNode getChildAt(int childIndex) {
        return getChildren().get(childIndex);
    }

    @Override
    public int getChildCount() {
        return getChildren().size();
    }

    @Override
    public int getIndex(TreeNode node) {
        return getChildren().indexOf((NavigatorNode) node);
    }

    @Override
    public boolean getAllowsChildren() {
        return getChildren().size() > 0;
    }

    @Override
    public boolean isLeaf() {
        return getChildren().size() == 0;
    }

    @Override
    public Enumeration<? extends TreeNode> children() {
        final Iterator<? extends NavigatorNode> it = getChildren().iterator();

        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public TreeNode nextElement() {
                return it.next();
            }
        };
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @NotNull
    public abstract String getLabel();

    @NotNull
    public abstract List<? extends NavigatorNode> getChildren();

    @Nullable
    @Override
    public abstract NavigatorNode getParent();
}
