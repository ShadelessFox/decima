package com.shade.decima.ui.navigator;

import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class NavigatorLazyNode extends NavigatorNode {
    private final List<NavigatorNode> children;
    private boolean loaded;
    private boolean loading;

    public NavigatorLazyNode() {
        this.children = new ArrayList<>();
        this.children.add(new LoadingNode());
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @NotNull
    @Override
    public List<NavigatorNode> getChildren() {
        return children;
    }

    public void loadChildren(@NotNull DefaultTreeModel model, @NotNull PropertyChangeListener listener) {
        if (loaded || loading) {
            return;
        }

        final SwingWorker<List<? extends NavigatorNode>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<? extends NavigatorNode> doInBackground() throws Exception {
                loading = true;

                try {
                    setProgress(0);
                    return loadChildren(listener);
                } finally {
                    setProgress(100);
                }
            }

            @Override
            protected void done() {
                try {
                    children.clear();
                    children.addAll(get());
                    model.nodeStructureChanged(NavigatorLazyNode.this);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    loading = false;
                    loaded = true;
                }
            }
        };

        worker.getPropertyChangeSupport().addPropertyChangeListener("progress", listener);
        worker.execute();
    }

    @NotNull
    protected abstract List<? extends NavigatorNode> loadChildren(@NotNull PropertyChangeListener listener) throws Exception;

    private class LoadingNode extends NavigatorNode {
        @Nullable
        @Override
        public String getLabel() {
            return "Loading ...";
        }

        @NotNull
        @Override
        public List<NavigatorNode> getChildren() {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public NavigatorNode getParent() {
            return NavigatorLazyNode.this;
        }
    }
}
