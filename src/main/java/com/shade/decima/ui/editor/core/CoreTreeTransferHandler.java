package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.editor.core.command.ElementMoveCommand;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

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
            final int sourceIndex = target.getIndex(source);
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
            final CoreNodeObject source = (CoreNodeObject) transferable.getTransferData(NodeTransferable.nodeFlavor);
            final CoreNodeObject target = (CoreNodeObject) location.getPath().getLastPathComponent();
            final int sourceIndex = target.getIndex(source);
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
        if (((JTree) c).getLastSelectedPathComponent() instanceof CoreNodeObject obj) {
            if (obj.getParent() instanceof CoreNodeObject par && par.getType() instanceof RTTITypeArray) {
                return MOVE;
            }
        }

        return NONE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        return new NodeTransferable((CoreNodeObject) ((JTree) c).getLastSelectedPathComponent());
    }

    private record NodeTransferable(@NotNull CoreNodeObject node) implements Transferable {
        private static final DataFlavor nodeFlavor = getNodeFlavor();
        private static final DataFlavor[] flavors = {nodeFlavor};

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return nodeFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (nodeFlavor.equals(flavor)) {
                return node;
            }

            throw new UnsupportedFlavorException(flavor);
        }

        @NotNull
        private static DataFlavor getNodeFlavor() {
            try {
                return new DataFlavor("application/octet-stream; class=" + CoreNodeObject.class.getName());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Error constructing flavor", e);
            }
        }
    }
}
