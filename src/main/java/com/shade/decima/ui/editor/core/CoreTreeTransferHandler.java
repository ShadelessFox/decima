package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.editor.core.command.ElementMoveCommand;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;

public class CoreTreeTransferHandler extends TransferHandler {
    private final CoreEditor editor;

    public CoreTreeTransferHandler(@NotNull CoreEditor editor) {
        this.editor = editor;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        try {
            final JTree.DropLocation location = (JTree.DropLocation) support.getDropLocation();
            final CoreTree tree = (CoreTree) support.getComponent();
            final CoreNodeObject source = (CoreNodeObject) support.getTransferable().getTransferData(NodeTransferable.nodeFlavor);
            final CoreNodeObject target = (CoreNodeObject) location.getPath().getLastPathComponent();
            final int sourceIndex = tree.getModel().getIndexOfChild(target, source);
            final int targetIndex = location.getChildIndex();

            editor.getCommandManager().add(new ElementMoveCommand(tree, target, sourceIndex, sourceIndex < targetIndex ? targetIndex - 1 : targetIndex));
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canImport(TransferSupport support) {
        final Transferable transferable = support.getTransferable();

        if (!support.isDrop() || !transferable.isDataFlavorSupported(NodeTransferable.nodeFlavor)) {
            return false;
        }

        try {
            final JTree.DropLocation location = (JTree.DropLocation) support.getDropLocation();
            final CoreTree tree = (CoreTree) support.getComponent();
            final CoreNodeObject source = (CoreNodeObject) transferable.getTransferData(NodeTransferable.nodeFlavor);
            final CoreNodeObject target = (CoreNodeObject) location.getPath().getLastPathComponent();
            final int sourceIndex = tree.getModel().getIndexOfChild(target, source);
            final int targetIndex = location.getChildIndex();

            return target.equals(source.getParent())
                && targetIndex != -1
                && targetIndex != sourceIndex
                && targetIndex != sourceIndex + 1;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getSourceActions(JComponent c) {
        final Object component = ((JTree) c).getLastSelectedPathComponent();

        if (component instanceof CoreNodeObject obj) {
            if (obj.getParent() instanceof CoreNodeObject par && par.getType() instanceof RTTITypeArray) {
                return COPY_OR_MOVE;
            }
        }

        if (component instanceof TreeNode) {
            return COPY;
        }

        return NONE;
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        if ((action & MOVE) == 0) {
            return;
        }

        super.exportAsDrag(comp, e, action);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        return new NodeTransferable((JTree) c, (TreeNode) ((JTree) c).getLastSelectedPathComponent());
    }

    private record NodeTransferable(@NotNull JTree tree, @NotNull TreeNode node) implements Transferable {
        private static final DataFlavor nodeFlavor = UIUtils.createLocalDataFlavor(TreeNode.class);
        private static final DataFlavor[] flavors = {nodeFlavor, DataFlavor.stringFlavor};

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor other) {
            for (DataFlavor flavor : flavors) {
                if (flavor.equals(other)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (nodeFlavor.equals(flavor)) {
                return node;
            } else if (DataFlavor.stringFlavor.equals(flavor)) {
                final int row = tree.getLeadSelectionRow();
                return tree.convertValueToText(node, true, tree.isExpanded(row), tree.getModel().isLeaf(node), row, true);
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }
}
