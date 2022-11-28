package com.shade.decima.ui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.FileEditorInputLazy;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

public class FindFileDialog extends JDialog {
    private static final int FILES_TO_DISPLAY = 100;

    public FindFileDialog(@NotNull JFrame frame, @NotNull Project project) {
        super(frame, "Find files", true);

        final List<FileInfo> files;

        try {
            files = buildFileInfoIndex(project);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final JTable table = new JTable(new FilterableTableModel(files, FILES_TO_DISPLAY));
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFocusable(false);
        table.getColumnModel().getColumn(0).setMaxWidth(100);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() % 2 == 0) {
                    final int row = table.rowAtPoint(e.getPoint());

                    if (row >= 0) {
                        final FilterableTableModel model = (FilterableTableModel) table.getModel();
                        final FileInfo info = model.getValueAt(row);

                        openSelectedFile(project, info);

                        if (!e.isControlDown()) {
                            dispose();
                        }
                    }
                }
            }
        });

        final JTextField input = new JTextField();
        input.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
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

        final JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(BorderFactory.createEmptyBorder());

        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(input, BorderLayout.NORTH);
        panel.add(tablePane, BorderLayout.CENTER);
        setContentPane(panel);

        pack();
        input.requestFocusInWindow();

        setSize(650, 350);
        setLocationRelativeTo(Application.getFrame());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        final JRootPane rootPane = getRootPane();

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final FilterableTableModel model = (FilterableTableModel) table.getModel();
                if (model.getRowCount() > 0) {
                    final FileInfo info = model.getValueAt(table.getSelectedRow());
                    openSelectedFile(project, info);
                    setVisible(false);
                }
            }
        });

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final FilterableTableModel model = (FilterableTableModel) table.getModel();
                if (model.getRowCount() > 0) {
                    final FileInfo info = model.getValueAt(table.getSelectedRow());
                    openSelectedFile(project, info);
                }
            }
        });
    }

    private void openSelectedFile(@NotNull Project project, @NotNull FileInfo info) {
        Application.getFrame().getEditorManager().openEditor(
            new FileEditorInputLazy(project.getContainer(), info.packfile(), info.path()),
            true
        );
    }

    @NotNull
    private List<FileInfo> buildFileInfoIndex(@NotNull Project project) throws IOException {
        final PackfileManager manager = project.getPackfileManager();
        final Map<Long, List<Packfile>> packfiles = buildFileHashToPackfilesMap(manager);

        final Set<Long> containing = new HashSet<>();
        final List<FileInfo> info = new ArrayList<>();

        try (Stream<String> files = project.listAllFiles()) {
            files.forEach(path -> {
                final long hash = PackfileBase.getPathHash(path);
                for (Packfile packfile : packfiles.getOrDefault(hash, Collections.emptyList())) {
                    info.add(new FileInfo(packfile, path, 0));
                    containing.add(hash);
                }
            });
        }

        for (Packfile packfile : manager.getPackfiles()) {
            for (PackfileBase.FileEntry entry : packfile.getFileEntries()) {
                if (!containing.contains(entry.hash())) {
                    info.add(new FileInfo(packfile, "<unnamed>/%8x".formatted(entry.hash()), entry.hash()));
                }
            }
        }

        return info;
    }

    @NotNull
    private Map<Long, List<Packfile>> buildFileHashToPackfilesMap(@NotNull PackfileManager manager) {
        final Map<Long, List<Packfile>> result = new HashMap<>();
        for (Packfile packfile : manager.getPackfiles()) {
            for (PackfileBase.FileEntry fileEntry : packfile.getFileEntries()) {
                result.computeIfAbsent(fileEntry.hash(), x -> new ArrayList<>()).add(packfile);
            }
        }
        return result;
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

            final long hash = PackfileBase.getPathHash(PackfileBase.getNormalizedPath(query, false));

            final List<FileInfo> output = choices.stream()
                .filter(x -> x.hash != 0 ? x.hash == hash : x.path.contains(query))
                .limit(limit)
                .toList();

            if (output.isEmpty()) {
                return;
            }

            results.addAll(output);
            fireTableRowsInserted(0, results.size());
        }
    }

    private record FileInfo(@NotNull Packfile packfile, @NotNull String path, long hash) {}
}
