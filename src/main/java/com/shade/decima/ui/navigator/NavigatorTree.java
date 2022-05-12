package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

public class NavigatorTree extends JScrollPane {
    private final NavigatorTreeModel model;
    private final JTree tree;

    public NavigatorTree(@NotNull NavigatorNode root) {
        this.model = new NavigatorTreeModel(this, root);
        this.tree = new JTree(model);
        this.tree.setCellRenderer(new NavigatorTreeCellRenderer());
        this.tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() % 2 == 0) {
                    final TreePath path = tree.getPathForLocation(e.getX(), e.getY());

                    if (path != null && path.getLastPathComponent() instanceof NavigatorNode.ActionListener l) {
                        l.actionPerformed(e);
                    }
                }
            }
        });
        this.tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && tree.getLastSelectedPathComponent() instanceof NavigatorNode.ActionListener l) {
                    l.actionPerformed(e);
                }
            }
        });

        setViewportView(tree);
    }

    @NotNull
    public JTree getTree() {
        return tree;
    }

    @NotNull
    public NavigatorTreeModel getModel() {
        return model;
    }

    @Nullable
    public NavigatorNode findFileNode(@NotNull ProgressMonitor monitor, @NotNull Project project, @NotNull Packfile packfile, @NotNull String[] path) throws Exception {
        NavigatorNode node = model.getRoot();

        node = findChild(monitor, node, child -> child instanceof NavigatorProjectNode n && n.getProject() == project);

        if (node == null) {
            return null;
        }

        node = findChild(monitor, node, child -> child instanceof NavigatorPackfileNode n && n.getPackfile() == packfile);

        if (node == null) {
            return null;
        }

        for (String part : path) {
            node = findChild(monitor, node, child -> child.getLabel().equals(part));

            if (node == null) {
                return null;
            }
        }

        return node;
    }

    @Nullable
    private NavigatorNode findChild(@NotNull ProgressMonitor monitor, @NotNull NavigatorNode root, @NotNull Predicate<NavigatorNode> predicate) throws Exception {
        for (NavigatorNode child : root.getChildren(monitor)) {
            if (predicate.test(child)) {
                return child;
            }
        }

        return null;
    }
}
