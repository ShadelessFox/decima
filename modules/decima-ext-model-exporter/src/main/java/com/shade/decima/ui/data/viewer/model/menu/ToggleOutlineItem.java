package com.shade.decima.ui.data.viewer.model.menu;

import com.shade.decima.model.viewer.MeshViewerCanvas;
import com.shade.decima.model.viewer.isr.impl.NodeModel;
import com.shade.decima.ui.data.viewer.model.outline.OutlineTree;
import com.shade.decima.ui.data.viewer.model.outline.OutlineTreeNode;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.util.Objects;
import java.util.Set;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = BAR_MODEL_VIEWER_ID, icon = "Action.outlineIcon", description = "Toggle outline", group = BAR_MODEL_VIEWER_GROUP_MISC, order = 2000)
public class ToggleOutlineItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final MeshViewerCanvas canvas = ctx.getData(MeshViewerCanvas.CANVAS_KEY);

        final JDialog dialog = new JDialog(JOptionPane.getRootFrame());
        dialog.setContentPane(createContentPane(canvas));
        dialog.setTitle("Overlay");
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(300, 400);
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setVisible(true);
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return ctx.getData(MeshViewerCanvas.CANVAS_KEY).getModel() instanceof NodeModel;
    }

    @NotNull
    private static JComponent createContentPane(@NotNull MeshViewerCanvas canvas) {
        final NodeModel model = (NodeModel) Objects.requireNonNull(canvas.getModel());

        final OutlineTree tree = new OutlineTree(model.getNode());
        tree.addTreeSelectionListener(e -> {
            if (tree.getLastSelectedPathComponent() instanceof OutlineTreeNode node) {
                canvas.setSelection(Set.of(node.getNode()));
            }
        });

        final JScrollPane pane = new JScrollPane(tree);
        pane.setBorder(null);

        return pane;
    }
}
