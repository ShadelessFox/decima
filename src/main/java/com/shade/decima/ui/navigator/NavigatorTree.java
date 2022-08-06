package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class NavigatorTree extends JTree {
    public NavigatorTree(@NotNull NavigatorNode root) {
        setModel(new NavigatorTreeModel(this, root));
        setCellRenderer(new NavigatorTreeCellRenderer(getModel()));
        setScrollsOnExpand(false);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() % 2 == 0) {
                    final TreePath path = getPathForLocation(e.getX(), e.getY());

                    if (path != null && path.getLastPathComponent() instanceof NavigatorNode.ActionListener l) {
                        l.actionPerformed(e);
                    }
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    final TreePath path = getSelectionPath();

                    if (path != null) {
                        if (path.getLastPathComponent() instanceof NavigatorNode.ActionListener l) {
                            l.actionPerformed(e);
                        }

                        if (!e.isConsumed()) {
                            togglePath(path);
                        }
                    }
                }
            }
        });
    }

    @NotNull
    public NavigatorTreeModel getModel() {
        return (NavigatorTreeModel) super.getModel();
    }

    public void togglePath(@NotNull TreePath path) {
        if (isExpanded(path)) {
            collapsePath(path);
        } else {
            expandPath(path);
        }
    }

    @NotNull
    public CompletableFuture<NavigatorFileNode> findFileNode(@NotNull ProgressMonitor monitor, @NotNull ProjectContainer container, @NotNull Packfile packfile, @NotNull String[] path) {
        CompletableFuture<NavigatorNode> future;

        future = findChild(
            monitor,
            getModel().getRoot(),
            child -> child instanceof NavigatorProjectNode n && n.getContainer().equals(container)
        );

        future = future.thenCompose(node -> findChild(
            monitor,
            node,
            child -> child instanceof NavigatorPackfileNode n && n.getPackfile().equals(packfile)
        ));

        for (String part : path) {
            future = future.thenCompose(node -> findChild(
                monitor,
                node,
                child -> child.getLabel().equals(part)
            ));
        }

        return future.thenApply(node -> (NavigatorFileNode) node);
    }

    @NotNull
    public CompletableFuture<NavigatorNode> findChild(@NotNull ProgressMonitor monitor, @NotNull NavigatorNode parent, @NotNull Predicate<NavigatorNode> predicate) {
        return getModel()
            .getChildrenAsync(monitor, parent)
            .thenApply(children -> {
                for (NavigatorNode child : children) {
                    if (predicate.test(child)) {
                        return child;
                    }
                }

                throw new IllegalArgumentException("Can't find node");
            });
    }

    @NotNull
    public CompletableFuture<NavigatorNode> findChild(@NotNull ProgressMonitor monitor, @NotNull Predicate<NavigatorNode> predicate) {
        return findChild(monitor, getModel().getRoot(), predicate);
    }
}
