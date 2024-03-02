package com.shade.platform.ui.controls.tree;

import com.formdev.flatlaf.ui.FlatTreeUI;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;

public class TreeUI extends FlatTreeUI {
    @SuppressWarnings("unused")
    @NotNull
    public static ComponentUI createUI(@NotNull JComponent c) {
        return new TreeUI();
    }

    @Override
    public boolean isLocationInExpandControl(TreePath path, int mouseX, int mouseY) {
        return super.isLocationInExpandControl(path, mouseX, mouseY);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void completeEditing(boolean messageStop, boolean messageCancel, boolean messageTree) {
        if (stopEditingInCompleteEditing && editingComponent != null) {
            final Component oldComponent = editingComponent;
            final TreePath oldPath = editingPath;
            final TreeCellEditor oldEditor = cellEditor;
            final Rectangle editingBounds = getPathBounds(tree, editingPath);
            final boolean requestFocus = tree.hasFocus() || SwingUtilities.findFocusOwner(editingComponent) != null;

            editingComponent = null;
            editingPath = null;

            if (messageStop) {
                oldEditor.stopCellEditing();
            } else if (messageCancel) {
                oldEditor.cancelCellEditing();
            }

            tree.remove(oldComponent);

            if (editorHasDifferentSize) {
                treeState.invalidatePathBounds(oldPath);
                updateSize();
            } else if (editingBounds != null) {
                editingBounds.x = 0;
                editingBounds.width = tree.getSize().width;
                tree.repaint(editingBounds);
            }

            if (requestFocus) {
                tree.requestFocus();
            }

            if (messageTree) {
                treeModel.valueForPathChanged(oldPath, oldEditor.getCellEditorValue());
            }
        }
    }

    public void invalidateSizes() {
        treeState.invalidateSizes();
        updateSize();
    }
}
