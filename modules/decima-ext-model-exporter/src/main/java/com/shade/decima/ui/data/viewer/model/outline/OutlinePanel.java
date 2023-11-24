package com.shade.decima.ui.data.viewer.model.outline;

import com.shade.decima.model.viewer.isr.Node;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

public class OutlinePanel extends JComponent {
    private final Node root;

    public OutlinePanel(@NotNull Node root) {
        this.root = root;

        final OutlineTree tree = new OutlineTree(root);
        final JScrollPane pane = new JScrollPane(tree);
        pane.setBorder(null);

        setLayout(new BorderLayout());
        add(pane, BorderLayout.CENTER);
    }
}
