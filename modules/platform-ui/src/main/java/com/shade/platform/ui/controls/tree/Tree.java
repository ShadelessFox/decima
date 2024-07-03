package com.shade.platform.ui.controls.tree;

import com.shade.platform.ui.controls.ColoredTreeCellRenderer;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Tree extends JTree {
    public Tree() {
        super((TreeModel) null);

        setScrollsOnExpand(false);
        setInvokesStopCellEditing(true);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        setModel(new TreeModel(this));

        final Handler handler = new Handler();
        addMouseListener(handler);
        addKeyListener(handler);

        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void togglePath(@NotNull TreePath path) {
        if (isExpanded(path)) {
            collapsePath(path);
        } else {
            expandPath(path);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (event != null) {
            final TreePath path = getPathForLocation(event.getX(), event.getY());

            if (path != null) {
                return ((TreeNode) path.getLastPathComponent()).getDescription();
            }
        }

        return null;
    }

    @Override
    @NotNull
    public TreeModel getModel() {
        return (TreeModel) super.getModel();
    }

    @Override
    public TreeUI getUI() {
        return (TreeUI) super.getUI();
    }

    @Override
    public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focused) {
        if (getCellRenderer() instanceof ColoredTreeCellRenderer<?> renderer) {
            return renderer
                .getTreeCellRendererComponent(this, value, selected, expanded, leaf, row, focused)
                .toString();
        }

        return super.convertValueToText(value, selected, expanded, leaf, row, focused);
    }

    private class Handler implements MouseListener, KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                return;
            }

            final TreePath[] paths = getSelectionPaths();

            if (paths != null && paths.length > 0) {
                for (TreePath path : paths) {
                    if (path.getLastPathComponent() instanceof TreeNode.ActionListener l) {
                        l.actionPerformed(e);
                    }
                }

                if (paths.length == 1 && !e.isConsumed()) {
                    togglePath(paths[0]);
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() % 2 != 0) {
                return;
            }

            final TreePath closestPath = getClosestPathForLocation(e.getX(), e.getY());
            if (closestPath != null && getUI().isLocationInExpandControl(closestPath, e.getX(), e.getY())) {
                return;
            }

            final TreePath[] paths = getSelectionPaths();
            if (paths != null) {
                for (TreePath path : paths) {
                    if (path.getLastPathComponent() instanceof TreeNode.ActionListener l) {
                        l.actionPerformed(e);
                    }
                }
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    }
}
