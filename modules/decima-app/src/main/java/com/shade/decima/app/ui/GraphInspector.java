package com.shade.decima.app.ui;

import com.shade.decima.app.ui.GraphStructure.Element;
import com.shade.decima.app.ui.tree.PaginatedTreeModel;
import com.shade.decima.app.ui.tree.StructuredTreeModel;
import com.shade.decima.app.ui.tree.Tree;
import com.shade.decima.app.ui.tree.TreeItem;
import com.shade.decima.app.ui.util.Fugue;
import com.shade.decima.game.hfw.storage.StreamingGraphResource;
import com.shade.decima.game.hfw.storage.StreamingObjectReader;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.runtime.ClassTypeInfo;
import com.shade.decima.rtti.runtime.TypedObject;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutionException;

public final class GraphInspector {
    static final Logger log = LoggerFactory.getLogger(GraphInspector.class);

    private GraphInspector() {
    }

    public static void show(@NotNull StreamingGraphResource graph, @NotNull StreamingObjectReader reader) {
        var dialog = new JDialog((Dialog) null);

        var model = new PaginatedTreeModel(10000, new StructuredTreeModel<>(new GraphStructure(graph)));
        var tree = new Tree(model);
        tree.setCellRenderer(new GraphTreeCellRenderer());
        tree.addActionListener(event -> {
            var component = tree.getLastSelectedPathComponent();
            var row = tree.getLeadSelectionRow();
            if (model.handleNextPage(component, event.getSource().isShiftDown())) {
                tree.setSelectionRow(row);
                return;
            }
            if (component instanceof TreeItem<?> item) {
                component = item.getValue();
            }
            if (component instanceof Element.Compound element) {
                tree.setEnabled(false);

                new SwingWorker<StreamingObjectReader.GroupResult, Object>() {
                    @Override
                    protected StreamingObjectReader.GroupResult doInBackground() throws Exception {
                        return reader.readGroup(element.group().groupID());
                    }

                    @Override
                    protected void done() {
                        try {
                            var result = get();
                            var object = result.root().objects().get(element.index());
                            SwingUtilities.invokeLater(() -> showObjectInfo(dialog, object.type(), object.object()));
                        } catch (ExecutionException e) {
                            log.error("Failed to read object", e);
                        } catch (InterruptedException ignored) {
                            // ignored
                        } finally {
                            tree.setEnabled(true);
                        }
                    }
                }.execute();
            }
        });

        installPopupMenu(tree);

        dialog.add(new JScrollPane(tree));
        dialog.setTitle("Graph Inspector");
        dialog.setSize(360, 720);
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);
        dialog.setVisible(true);
        dialog.dispose();
    }

    private static void installPopupMenu(@NotNull Tree tree) {
        var menu = new JPopupMenu();
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                var path = tree.getSelectionPath();
                if (path == null) {
                    return;
                }
                var component = path.getLastPathComponent();
                if (component instanceof TreeItem<?> item) {
                    component = item.getValue();
                }
                if (component instanceof Element.GroupObjects objects) {
                    var groupObjectsByType = new JCheckBoxMenuItem("Group objects by type");
                    groupObjectsByType.setSelected(objects.options().contains(Element.GroupObjects.Options.GROUP_BY_TYPE));
                    groupObjectsByType.addActionListener(e -> {
                        if (((JCheckBoxMenuItem) e.getSource()).isSelected()) {
                            objects.options().add(Element.GroupObjects.Options.GROUP_BY_TYPE);
                        } else {
                            objects.options().remove(Element.GroupObjects.Options.GROUP_BY_TYPE);
                        }
                        tree.getModel().unload(path.getLastPathComponent());
                        tree.getModel().nodeStructureChanged(path);
                    });
                    menu.add(groupObjectsByType);

                    if (groupObjectsByType.isSelected()) {
                        var sortByCount = new JCheckBoxMenuItem("Sort by count");
                        sortByCount.setSelected(objects.options().contains(Element.GroupObjects.Options.SORT_BY_COUNT));
                        sortByCount.addActionListener(e -> {
                            if (((JCheckBoxMenuItem) e.getSource()).isSelected()) {
                                objects.options().add(Element.GroupObjects.Options.SORT_BY_COUNT);
                            } else {
                                objects.options().remove(Element.GroupObjects.Options.SORT_BY_COUNT);
                            }
                            tree.getModel().unload(path.getLastPathComponent());
                            tree.getModel().nodeStructureChanged(path);
                        });
                        menu.add(sortByCount);
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                menu.removeAll();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                menu.removeAll();
            }
        });
        tree.setComponentPopupMenu(menu);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    var path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        tree.setSelectionPath(path);
                        tree.scrollPathToVisible(path);
                    }
                }
            }
        });
    }

    private static void showObjectInfo(@NotNull Window parent, @NotNull ClassTypeInfo info, @NotNull Object object) {
        var dialog = new JDialog(parent, "Object Inspector", Dialog.ModalityType.APPLICATION_MODAL);

        var model = new PaginatedTreeModel(100, new StructuredTreeModel<>(new ObjectStructure(info, object)));
        var tree = new Tree(model);
        tree.addActionListener(event -> {
            var component = event.getLastPathComponent();
            var row = tree.getLeadSelectionRow();
            if (component == null) {
                return;
            }
            if (model.handleNextPage(component, event.getSource().isShiftDown())) {
                tree.setSelectionRow(row);
                return;
            }
            if (component instanceof TreeItem<?> wrapper) {
                component = wrapper.getValue();
            }
            if (component instanceof ObjectStructure.Element element
                && element.value() instanceof Ref<?> ref
                && ref.get() instanceof TypedObject target
            ) {
                showObjectInfo(dialog, target.getType(), target);
            }
        });

        var pane = dialog.getRootPane();
        pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        pane.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Window ancestor = SwingUtilities.getWindowAncestor((Component) e.getSource());
                if (ancestor != null) {
                    ancestor.setVisible(false);
                }
            }
        });

        dialog.add(new JScrollPane(tree));
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    private static class GraphTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            Object node = value;
            if (value instanceof TreeItem<?> item) {
                node = item.getValue();
            }
            if (node instanceof Element element) {
                var icon = switch (element) {
                    case Element.Root ignored -> Fugue.getIcon("folders-stack");
                    case Element.Group ignored -> Fugue.getIcon("folders");
                    case Element.GroupDependentGroups ignored -> Fugue.getIcon("folder-import");
                    case Element.GroupDependencyGroups ignored -> Fugue.getIcon("folder-export");
                    case Element.GroupRoots ignored -> Fugue.getIcon("folder-bookmark");
                    case Element.GroupObjects ignored -> Fugue.getIcon("folder-open-document");
                    case Element.GroupObjectSet ignored -> Fugue.getIcon("folder-open-document");
                    case Element.Compound ignored -> Fugue.getIcon("blue-document");
                };
                if (tree.isEnabled()) {
                    setIcon(icon);
                } else {
                    Icon disabledIcon = UIManager.getLookAndFeel().getDisabledIcon(tree, icon);
                    setDisabledIcon(disabledIcon != null ? disabledIcon : icon);
                }
            }

            return this;
        }
    }
}
