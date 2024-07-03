package com.shade.decima.model.viewer.outline;

import com.shade.decima.model.viewer.isr.Node;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public class OutlineDialog extends JDialog {
    private final Set<Node> selection = new HashSet<>();

    public OutlineDialog(@NotNull Window owner, @NotNull Node root) {
        super(owner, "Outline");

        final OutlineTree tree = new OutlineTree(root);
        tree.addTreeSelectionListener(e -> {
            selection.clear();

            if (tree.getLastSelectedPathComponent() instanceof OutlineTreeNode node) {
                selection.add(node.getNode());
            }
        });

        setContentPane(UIUtils.createBorderlessScrollPane(tree));
        setSize(300, 400);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setLocationRelativeTo(owner);

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
    }

    @NotNull
    public Set<Node> getSelection() {
        return selection;
    }
}
