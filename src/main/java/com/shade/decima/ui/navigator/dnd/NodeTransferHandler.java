package com.shade.decima.ui.navigator.dnd;

import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NodeTransferHandler extends TransferHandler {
    private static final Logger log = LoggerFactory.getLogger(NodeTransferHandler.class);

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (!(c instanceof JTree tree)) {
            return null;
        }

        final TreePath[] paths = tree.getSelectionPaths();

        if (paths != null && paths.length > 0) {
            final List<NavigatorFileNode> nodes = new ArrayList<>();

            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof NavigatorFileNode node) {
                    nodes.add(node);
                }
            }

            if (!nodes.isEmpty()) {
                return new NodeTransferable(nodes.toArray(NavigatorFileNode[]::new));
            }
        }

        return null;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        try {
            ((NodeTransferable) data).close();
        } catch (IOException e) {
            log.error("Unable to clean up the transferable", e);
        }
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        super.exportToClipboard(comp, clip, action);
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        if ((action & MOVE) == 0) {
            return;
        }

        super.exportAsDrag(comp, e, action);
    }

    @Override
    public int getSourceActions(JComponent c) {
        if (!(c instanceof JTree tree)) {
            return NONE;
        }

        final TreePath[] paths = tree.getSelectionPaths();

        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof NavigatorFileNode) {
                    return COPY_OR_MOVE;
                }
            }
        }

        return NONE;
    }
}
