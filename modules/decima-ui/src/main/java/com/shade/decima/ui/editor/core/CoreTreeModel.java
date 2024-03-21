package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.ui.data.MutableValueController;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;
import java.util.concurrent.CompletableFuture;

public class CoreTreeModel extends TreeModel {
    public CoreTreeModel(@NotNull Tree tree, @NotNull TreeNode root) {
        super(tree, root);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object value) {
        final CoreTreeCellEditor editor = (CoreTreeCellEditor) tree.getCellEditor();
        final MutableValueController<Object> controller = editor.getController();

        controller.setValue(value);
    }

    @NotNull
    public CompletableFuture<? extends TreeNode> findNode(@NotNull ProgressMonitor monitor, @NotNull RTTIPath path) {
        CompletableFuture<? extends TreeNode> future = null;

        assert path.elements().length > 0;

        final CoreNodeFile root = (CoreNodeFile) getRoot();

        for (RTTIPathElement element : path.elements()) {
            if (future == null && root.isGroupingEnabled()) {
                future = findChild(
                    monitor,
                    root,
                    child -> ((CoreNodeEntryGroup) child).contains((RTTIPathElement.UUID) element)
                );
            }
            if (future == null) {
                future = findChild(
                    monitor,
                    root,
                    child -> matches(element, child)
                );
            } else {
                future = future.thenCompose(node -> findChild(
                    monitor,
                    node,
                    child -> matches(element, child)
                ));
            }
        }

        return future;
    }

    private static boolean matches(@NotNull RTTIPathElement element, @NotNull TreeNode node) {
        return node instanceof CoreNodeObject object && object.getPath().endsWith(element);
    }
}
