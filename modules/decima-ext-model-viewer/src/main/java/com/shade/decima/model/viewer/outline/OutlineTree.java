package com.shade.decima.model.viewer.outline;

import com.shade.decima.model.viewer.isr.Node;
import com.shade.decima.model.viewer.outline.menu.OutlineMenuConstants;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

public class OutlineTree extends Tree {
    public OutlineTree(@NotNull Node root) {
        super(new OutlineTreeNode(root), TreeModel::new);
        setCellRenderer(new OutlineTreeCellRenderer());
        setSelectionPath(new TreePath(getModel().getRoot()));

        MenuManager.getInstance().installContextMenu(this, OutlineMenuConstants.CTX_MENU_SCENE_OUTLINE_ID, key -> switch (key) {
            case "selection" -> getLastSelectedPathComponent();
            default -> null;
        });
    }
}
