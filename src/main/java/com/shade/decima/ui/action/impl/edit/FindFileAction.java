package com.shade.decima.ui.action.impl.edit;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTICollection;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;

// TODO: This action is always enabled despite the context surrounding it.
//       Although we make sure that currently selected node is present and
//       loaded, it would be cool if the action API could handle it automatically

@ActionRegistration(name = "Find &Files\u2026", description = "Find files that match the string", accelerator = "ctrl shift F")
@ActionContribution(path = "menu:edit")
public class FindFileAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final NavigatorTree navigator = Application.getFrame().getNavigator();

        if (navigator.getTree().getLastSelectedPathComponent() instanceof NavigatorNode node) {
            final NavigatorProjectNode root = UIUtils.getParentNode(node, NavigatorProjectNode.class);
            if (root.needsInitialization()) {
                return;
            }
            new FindFileDialog(Application.getFrame(), root).setVisible(true);
        }
    }

    private static class FindFileDialog extends JDialog {

        public FindFileDialog(@NotNull JFrame frame, @NotNull NavigatorNode root) {
            super(frame, "Find files", true);

            final Project project = UIUtils.getProject(root);
            final List<FileInfo> files;

            try {
                files = buildFileInfoIndex(project);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final JTable table = new JTable(new FilterableTableModel(files, 100));
            table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setFocusable(false);
            table.getColumnModel().getColumn(0).setMaxWidth(100);
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() % 2 == 0) {
                        final int row = table.rowAtPoint(e.getPoint());

                        if (row >= 0) {
                            final FilterableTableModel model = (FilterableTableModel) table.getModel();
                            final FileInfo info = model.getValueAt(row);

                            openSelectedFile(project, info);
                            setVisible((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0);
                        }
                    }
                }
            });

            final JTextField input = new JTextField();
            input.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter part of a name");
            input.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSearchIcon());
            input.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    changedUpdate(e);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    ((FilterableTableModel) table.getModel()).refresh(PackfileBase.getNormalizedPath(input.getText(), false));
                    table.changeSelection(0, 0, false, false);
                }
            });

            UIUtils.delegateAction(input, table, "selectPreviousRow", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            UIUtils.delegateAction(input, table, "selectNextRow", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            UIUtils.delegateAction(input, table, "scrollUpChangeSelection", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            UIUtils.delegateAction(input, table, "scrollDownChangeSelection", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            UIUtils.delegateAction(input, table, "selectFirstRow", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            UIUtils.delegateAction(input, table, "selectLastRow", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

            final JPanel panel = new JPanel();
            panel.setLayout(new MigLayout("ins dialog", "[fill,grow]", "[][fill,grow]"));
            panel.add(input, "wrap");
            panel.add(new JScrollPane(table));
            getContentPane().add(panel);

            pack();
            input.requestFocusInWindow();

            setSize(650, 350);
            setLocationRelativeTo(Application.getFrame());
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            final JRootPane rootPane = getRootPane();

            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
            rootPane.getActionMap().put("close", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openAndClose");
            rootPane.getActionMap().put("openAndClose", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    final FilterableTableModel model = (FilterableTableModel) table.getModel();
                    final FileInfo info = model.getValueAt(table.getSelectedRow());
                    openSelectedFile(project, info);
                    setVisible(false);
                }
            });

            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "openAndKeep");
            rootPane.getActionMap().put("openAndKeep", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    final FilterableTableModel model = (FilterableTableModel) table.getModel();
                    final FileInfo info = model.getValueAt(table.getSelectedRow());
                    openSelectedFile(project, info);
                }
            });
        }

        private void openSelectedFile(@NotNull Project project, @NotNull FileInfo info) {
            final NavigatorNode target;

            try {
                target = Application.getFrame().getNavigator().findFileNode(new VoidProgressMonitor(), project, info.packfile, info.path.split("/"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (target instanceof NavigatorFileNode file) {
                Application.getFrame().getEditorsPane().showEditor(file);
            }
        }

        @NotNull
        private List<FileInfo> buildFileInfoIndex(@NotNull Project project) throws IOException {
            final PackfileManager manager = project.getPackfileManager();
            final Packfile prefetchPackfile = manager.findAny("prefetch/fullgame.prefetch");

            if (prefetchPackfile == null) {
                return Collections.emptyList();
            }

            final CoreBinary binary = CoreBinary.from(prefetchPackfile.extract("prefetch/fullgame.prefetch"), project.getTypeRegistry());

            if (binary.isEmpty()) {
                return Collections.emptyList();
            }

            final RTTIObject prefetch = binary.entries().get(0);
            final List<FileInfo> info = new ArrayList<>();

            final Map<Long, List<Packfile>> packfiles = new HashMap<>();

            for (Packfile packfile : manager.getPackfiles()) {
                for (PackfileBase.FileEntry entry : packfile.getFileEntries()) {
                    packfiles
                        .computeIfAbsent(entry.hash(), x -> new ArrayList<>())
                        .add(packfile);
                }
            }

            for (RTTIObject file : prefetch.<RTTICollection<RTTIObject>>get("Files")) {
                final String path = PackfileBase.getNormalizedPath(file.get("Path"));
                final long hash = PackfileBase.getPathHash(path);

                for (Packfile packfile : packfiles.getOrDefault(hash, Collections.emptyList())) {
                    info.add(new FileInfo(packfile, path, hash));
                }
            }

            return info;
        }
    }

    public static class FilterableTableModel extends AbstractTableModel {
        private final List<FileInfo> choices;
        private final List<FileInfo> results;
        private final int limit;

        public FilterableTableModel(@NotNull List<FileInfo> choices, int limit) {
            this.choices = choices;
            this.results = new ArrayList<>();
            this.limit = limit;
        }

        @Override
        public int getRowCount() {
            return results.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Packfile";
                case 1 -> "Path";
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final FileInfo info = results.get(rowIndex);

            return switch (columnIndex) {
                case 0 -> info.packfile.getName();
                case 1 -> info.path;
                default -> null;
            };
        }

        @NotNull
        public FileInfo getValueAt(int rowIndex) {
            return results.get(rowIndex);
        }

        public void refresh(@NotNull String query) {
            final int size = results.size();

            results.clear();
            fireTableRowsDeleted(0, size);

            if (query.isEmpty()) {
                return;
            }

            final List<FileInfo> output = choices.stream()
                .filter(x -> x.path.contains(query))
                .limit(limit)
                .toList();

            if (output.isEmpty()) {
                return;
            }

            results.addAll(output);
            fireTableRowsInserted(0, results.size());
        }
    }

    private static record FileInfo(@NotNull Packfile packfile, @NotNull String path, long hash) {
    }
}
