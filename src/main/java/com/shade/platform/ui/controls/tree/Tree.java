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
import java.util.function.BiFunction;

public class Tree extends JTree {
    public Tree(@NotNull TreeNode root, @NotNull BiFunction<Tree, TreeNode, TreeModel> model) {
        setModel(model.apply(this, root));
        setScrollsOnExpand(false);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        final Handler handler = new Handler();
        addMouseListener(handler);
        addKeyListener(handler);
    }

    public void togglePath(@NotNull TreePath path) {
        if (isExpanded(path)) {
            collapsePath(path);
        } else {
            expandPath(path);
        }
    }

    @NotNull
    public TreeModel getModel() {
        return (TreeModel) super.getModel();
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
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                final TreePath path = getSelectionPath();

                if (path != null) {
                    if (path.getLastPathComponent() instanceof TreeNode.ActionListener l) {
                        l.actionPerformed(e);
                    }

                    if (!e.isConsumed()) {
                        togglePath(path);
                    }
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() % 2 == 0) {
                final TreePath path = getPathForLocation(e.getX(), e.getY());

                if (path != null && path.getLastPathComponent() instanceof TreeNode.ActionListener l) {
                    l.actionPerformed(e);
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
