package com.shade.decima.ui.navigator.dnd;

import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Transferable;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileTransferHandler extends TransferHandler {
    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JTree tree) {
            final TreePath[] paths = tree.getSelectionPaths();
            if (paths != null && paths.length > 0) {
                final List<NavigatorFileNode> nodes = new ArrayList<>();
                for (TreePath path : paths) {
                    if (path.getLastPathComponent() instanceof NavigatorFileNode node) {
                        nodes.add(node);
                    }
                }
                if (!nodes.isEmpty()) {
                    return new FileTransferable(nodes);
                }
            }
        }

        return null;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if (data instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public int getSourceActions(JComponent c) {
        if (c instanceof JTree tree) {
            final TreePath path = tree.getSelectionPath();
            if (path != null && path.getLastPathComponent() instanceof NavigatorFileNode) {
                return COPY;
            }
        }

        return NONE;
    }
}
