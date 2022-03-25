package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class NavigatorLazyNode extends NavigatorNode {
    private NavigatorNode[] children;

    public NavigatorLazyNode(@Nullable NavigatorNode parent) {
        super(parent);
    }

    @NotNull
    @Override
    public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
        return getChildren(monitor, null);
    }

    @NotNull
    public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor, @Nullable NavigatorTreeModel model) throws Exception {
        if (needsInitialization()) {
            children = loadChildren(monitor);
        }

        if (model != null) {
            // TODO: Very inefficient!

            final List<NavigatorNode> result = new ArrayList<>();
            final Map<Object, List<NavigatorNode>> container = new LinkedHashMap<>();

            for (NavigatorNode child : children) {
                final Object key = model.getClassifierKey(NavigatorLazyNode.this, child);
                if (key != null) {
                    container.computeIfAbsent(key, x -> new ArrayList<>()).add(child);
                } else {
                    result.add(child);
                }
            }

            for (Map.Entry<Object, List<NavigatorNode>> entry : container.entrySet()) {
                result.add(model.getClassifierNode(NavigatorLazyNode.this, entry.getKey(), entry.getValue().toArray(NavigatorNode[]::new)));
            }

            children = result.toArray(NavigatorNode[]::new);
        }

        return children;
    }

    public boolean needsInitialization() {
        return children == null;
    }

    @NotNull
    protected abstract NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception;

    public static class LoadingNode extends NavigatorNode {
        public LoadingNode(@Nullable NavigatorNode parent) {
            super(parent);
        }

        @NotNull
        @Override
        public String getLabel() {
            return "Loading ...";
        }

        @NotNull
        @Override
        public NavigatorNode[] getChildren(@NotNull ProgressMonitor monitor) throws Exception {
            return new NavigatorNode[0];
        }
    }
}
