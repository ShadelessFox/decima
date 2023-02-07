package com.shade.decima.ui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.FileEditorInputLazy;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.dialogs.ProgressDialog;
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
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

public class FindFilesDialog extends JDialog {
    private static final WeakHashMap<Project, WeakReference<FileInfo[]>> CACHE = new WeakHashMap<>();

    private FileInfo[] files;

    public FindFilesDialog(@NotNull JFrame frame, @NotNull Project project) {
        super(frame, "Find files", true);

        if (CACHE.containsKey(project)) {
            final WeakReference<FileInfo[]> ref = CACHE.get(project);
            if (ref != null) {
                files = ref.get();
            }
        }

        if (files == null) {
            try {
                files = ProgressDialog
                    .showProgressDialog(frame, "Build file info index", monitor -> buildFileInfoIndex(monitor, project))
                    .orElse(null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            if (files != null) {
                CACHE.put(project, new WeakReference<>(files));
            }
        }

        if (files == null) {
            dispose();
            return;
        }

        final JTable table = new JTable(new FilterableTableModel(files));
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
                dispose();
            }
        });

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final FilterableTableModel model = (FilterableTableModel) table.getModel();
                if (model.getRowCount() > 0) {
                    final FileInfo info = model.getValueAt(table.getSelectedRow());
                    openSelectedFile(project, info);
                    dispose();
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

    public boolean hasFilesToShow() {
        return files != null;
    }

    private void openSelectedFile(@NotNull Project project, @NotNull FileInfo info) {
        Application.getFrame().getEditorManager().openEditor(
            new FileEditorInputLazy(project.getContainer(), info.packfile(), info.path()),
            true
        );
    }

    @NotNull
    private static FileInfo[] buildFileInfoIndex(@NotNull ProgressMonitor monitor, @NotNull Project project) throws IOException {
        try (var task = monitor.begin("Build file info index", 2)) {
            final PackfileManager manager = project.getPackfileManager();
            final Map<Long, List<Packfile>> packfiles = buildFileHashToPackfilesMap(manager);

            final Set<Long> containing = new HashSet<>();
            final List<FileInfo> info = new ArrayList<>();

            try (var ignored = task.split(1).begin("Add named entries")) {
                try (Stream<String> files = project.listAllFiles()) {
                    files.forEach(path -> {
                        final long hash = PackfileBase.getPathHash(path);
                        for (Packfile packfile : packfiles.getOrDefault(hash, Collections.emptyList())) {
                            info.add(new FileInfo(packfile, path, 0));
                            containing.add(hash);
                        }
                    });
                }
            }

            try (var ignored = task.split(1).begin("Add unnamed entries")) {
                for (Packfile packfile : manager.getPackfiles()) {
                    for (PackfileBase.FileEntry entry : packfile.getFileEntries()) {
                        if (!containing.contains(entry.hash())) {
                            info.add(new FileInfo(packfile, "<unnamed>/%8x".formatted(entry.hash()), entry.hash()));
                        }
                    }
                }
            }

            return info.toArray(FileInfo[]::new);
        }
    }

    @NotNull
    private static Map<Long, List<Packfile>> buildFileHashToPackfilesMap(@NotNull PackfileManager manager) {
        final Map<Long, List<Packfile>> result = new HashMap<>();
        for (Packfile packfile : manager.getPackfiles()) {
            for (PackfileBase.FileEntry fileEntry : packfile.getFileEntries()) {
                result.computeIfAbsent(fileEntry.hash(), x -> new ArrayList<>()).add(packfile);
            }
        }
        return result;
    }

    public static class FilterableTableModel extends AbstractTableModel {
        private static final FileInfo[] NO_RESULTS = new FileInfo[0];
        private static final int MAX_RESULTS = 1000;

        private final FileInfo[] choices;
        private FileInfo[] results;

        public FilterableTableModel(@NotNull FileInfo[] choices) {
            this.choices = choices;
            this.results = NO_RESULTS;
        }

        @Override
        public int getRowCount() {
            return results.length;
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
            final FileInfo info = results[rowIndex];

            return switch (columnIndex) {
                case 0 -> info.packfile.getName();
                case 1 -> info.path;
                default -> null;
            };
        }

        @NotNull
        public FileInfo getValueAt(int rowIndex) {
            return results[rowIndex];
        }

        public void refresh(@NotNull String query) {
            final int size = results.length;

            results = NO_RESULTS;
            fireTableRowsDeleted(0, size);

            if (query.isEmpty()) {
                return;
            }

            final long hash = PackfileBase.getPathHash(PackfileBase.getNormalizedPath(query, false));

            final FileInfo[] output = Arrays.stream(choices)
                .filter(x -> x.hash != 0 && x.hash == hash || x.path.contains(query))
                .limit(MAX_RESULTS)
                .toArray(FileInfo[]::new);

            if (output.length == 0) {
                return;
            }

            results = output;
            fireTableRowsInserted(0, results.length);
        }
    }

    private record FileInfo(@NotNull Packfile packfile, @NotNull String path, long hash) {}
}
