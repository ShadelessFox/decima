package com.shade.platform.ui.controls;

import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class BreadcrumbBar extends JComponent implements TreeSelectionListener, ActionListener {
    private final ButtonGroup group;
    private final Tree tree;

    public BreadcrumbBar(@NotNull Tree tree) {
        this.group = new ButtonGroup();
        this.tree = tree;
        this.tree.addTreeSelectionListener(this);

        setLayout(new FlowLayout(FlowLayout.LEADING, 2, 2));
        setPath(tree.getModel().getTreePathToRoot(tree.getModel().getRoot()), false);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        final TreePath path = e.getNewLeadSelectionPath();

        if (path != null) {
            setPath(path, true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final TreePath path = getActivePath();
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    public void setPath(@NotNull TreePath path, boolean keepChildNodes) {
        final Object[] elements = path.getPath();
        final Component[] components = getComponents();
        int i;

        for (i = 0; i < Math.min(components.length, elements.length); i++) {
            if (!((ItemButton) components[i]).node.equals(elements[i])) {
                break;
            }
        }

        final ItemButton selection;

        if (i < elements.length || !keepChildNodes) {
            for (int j = i; j < components.length; j++) {
                final ItemButton item = (ItemButton) components[j];
                item.removeActionListener(this);
                remove(item);
                group.remove(item);
            }

            for (int j = i; j < elements.length; j++) {
                final ItemButton item = new ItemButton((TreeNode) elements[j]);
                item.addActionListener(this);
                add(item);
                group.add(item);
            }

            revalidate();
            repaint();

            selection = (ItemButton) getComponent(elements.length - 1);
        } else {
            selection = (ItemButton) getComponent(i - 1);
        }

        group.setSelected(selection.getModel(), true);
    }

    @NotNull
    public TreePath getActivePath() {
        final List<TreeNode> nodes = new ArrayList<>();

        for (int i = 0; i < getComponentCount(); i++) {
            final ItemButton item = (ItemButton) getComponent(i);

            nodes.add(item.node);

            if (item.isSelected()) {
                break;
            }
        }

        return new TreePath(nodes.toArray());
    }

    private static class ItemButton extends JToggleButton {
        private final TreeNode node;

        public ItemButton(@NotNull TreeNode node) {
            super(node.getLabel(), node.getIcon());
            this.node = node;

            setFocusable(false);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Button.borderColor")),
                BorderFactory.createEmptyBorder(1, 4, 1, 4)
            ));
        }
    }
}
