package com.shade.platform.ui.controls.tree;

import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Tree extends JTree {
    public Tree(@NotNull TreeNode root) {
        setModel(new TreeModel(this, root));
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
