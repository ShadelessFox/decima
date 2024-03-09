package com.shade.decima.ui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchWithHistoryIcon;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.ui.editor.NodeEditorInputLazy;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.DocumentAdapter;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.util.EmptyAction;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FindFilesDialog extends JDialog {
    public enum Strategy {
        FIND_MATCHING("Find matching\u2026", "Enter full path to a file, part of its name, or hash (0xDEADBEEF)", UIManager.getIcon("Action.containsIcon")),
        FIND_REFERENCED_BY("Find referenced by\u2026", "Enter full path to a file to find files that reference it", UIManager.getIcon("Action.exportIcon")),
        FIND_REFERENCES_TO("Find references to\u2026", "Enter full path to a file to find files that are referenced by it", UIManager.getIcon("Action.importIcon"));

        private final String label;
        private final String placeholder;
        private final Icon icon;

        Strategy(@NotNull String label, @NotNull String placeholder, @NotNull Icon icon) {
            this.label = label;
            this.placeholder = placeholder;
            this.icon = icon;
        }
    }

    private static final Pattern HASH_PATTERN = Pattern.compile("0x([a-fA-F0-9]{12,16})");
    private static final WeakHashMap<Project, WeakReference<FileInfoIndex>> CACHE = new WeakHashMap<>();
    private static final WeakHashMap<Project, Deque<HistoryRecord>> HISTORY = new WeakHashMap<>();
    private static final int HISTORY_LIMIT = 10;

    private final Project project;

    private final JComboBox<Strategy> strategyCombo;
    private final JTextField inputField;
    private final JTable resultsTable;
    private final JToolBar statusBar;
    private final JLabel statusLabel;

    public static void show(@NotNull Frame frame, @NotNull Project project, @NotNull Strategy strategy, @Nullable String query) {
        FileInfoIndex index = null;

        if (CACHE.containsKey(project)) {
            final WeakReference<FileInfoIndex> ref = CACHE.get(project);
            if (ref != null) {
                index = ref.get();
            }
        }

        if (index == null) {
            try {
                index = ProgressDialog
                    .showProgressDialog(frame, "Build file info index", monitor -> buildFileInfoIndex(monitor, project))
                    .orElse(null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            if (index != null) {
                CACHE.put(project, new WeakReference<>(index));
            }
        }

        if (index == null) {
            return;
        }

        new FindFilesDialog(frame, project, strategy, index, query).setVisible(true);
    }

    private FindFilesDialog(@NotNull Frame frame, @NotNull Project project, @NotNull Strategy initialStrategy, @NotNull FileInfoIndex index, @Nullable String query) {
        super(frame, "Find Files in '%s'".formatted(project.getContainer().getName()), true);
        this.project = project;

        resultsTable = new JTable(new FilterableTableModel(index));
        resultsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setFocusable(false);
        resultsTable.getColumnModel().getColumn(0).setMaxWidth(100);
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        resultsTable.getColumnModel().getColumn(2).setMaxWidth(100);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() % 2 == 0) {
                    final int row = resultsTable.rowAtPoint(e.getPoint());

                    if (row >= 0) {
                        final FilterableTableModel model = (FilterableTableModel) resultsTable.getModel();
                        final FileInfo info = model.getValueAt(row);

                        openSelectedFile(project, info);

                        if (!e.isControlDown()) {
                            dispose();
                        }
                    }
                }
            }
        });

        inputField = new JTextField();
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, UIColor.SHADOW),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        inputField.getDocument().addDocumentListener((DocumentAdapter) e -> refreshResults());

        strategyCombo = new JComboBox<>(Strategy.values());
        strategyCombo.setSelectedItem(initialStrategy);
        strategyCombo.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, UIColor.SHADOW));
        strategyCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Strategy> list, @NotNull Strategy value, int index, boolean selected, boolean focused) {
                setLeadingIcon(value.icon);
                append(value.label, TextAttributes.REGULAR_ATTRIBUTES);
            }
        });
        strategyCombo.addItemListener(e -> {
            final Strategy strategy = strategyCombo.getItemAt(strategyCombo.getSelectedIndex());
            inputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, strategy.placeholder);
            refreshResults();
        });

        final JToolBar toolbar = new JToolBar();
        toolbar.add(strategyCombo);
        toolbar.add(new SearchHistoryAction());

        inputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, initialStrategy.placeholder);
        inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, toolbar);

        UIUtils.delegateAction(inputField, resultsTable, "selectPreviousRow", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        UIUtils.delegateAction(inputField, resultsTable, "selectNextRow", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        UIUtils.delegateAction(inputField, resultsTable, "scrollUpChangeSelection", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        UIUtils.delegateAction(inputField, resultsTable, "scrollDownChangeSelection", JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        statusLabel = new JLabel();

        statusBar = new JToolBar();
        statusBar.add(statusLabel);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UIColor.SHADOW),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        statusBar.setVisible(false);

        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(inputField, BorderLayout.NORTH);
        panel.add(UIUtils.createBorderlessScrollPane(resultsTable), BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);
        setContentPane(panel);

        pack();
        inputField.requestFocusInWindow();

        setSize(650, 350);
        setLocationRelativeTo(frame);
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
                final FilterableTableModel model = (FilterableTableModel) resultsTable.getModel();
                if (model.getRowCount() > 0) {
                    final FileInfo info = model.getValueAt(resultsTable.getSelectedRow());
                    openSelectedFile(project, info);
                    dispose();
                }
            }
        });

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                final FilterableTableModel model = (FilterableTableModel) resultsTable.getModel();
                if (model.getRowCount() > 0) {
                    final FileInfo info = model.getValueAt(resultsTable.getSelectedRow());
                    openSelectedFile(project, info);
                }
            }
        });

        if (query != null) {
            inputField.setText(query);
            inputField.selectAll();
        }
    }

    private void refreshResults() {
        ((FilterableTableModel) resultsTable.getModel()).refresh(
            Packfile.getNormalizedPath(inputField.getText(), false),
            strategyCombo.getItemAt(strategyCombo.getSelectedIndex())
        );
        resultsTable.changeSelection(0, 0, false, false);
    }

    private void openSelectedFile(@NotNull Project project, @NotNull FileInfo info) {
        EditorManager.getInstance().openEditor(
            new NodeEditorInputLazy(project.getContainer(), info.packfile(), info.path()),
            true
        );

        final Deque<HistoryRecord> history = HISTORY.computeIfAbsent(project, x -> new ArrayDeque<>());
        final HistoryRecord record = new HistoryRecord(inputField.getText(), strategyCombo.getItemAt(strategyCombo.getSelectedIndex()));
        history.remove(record);
        history.offerFirst(record);
        if (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    @NotNull
    private static FileInfoIndex buildFileInfoIndex(@NotNull ProgressMonitor monitor, @NotNull Project project) throws IOException {
        try (var task = monitor.begin("Build file info index", 3)) {
            final List<FileInfo> info = new ArrayList<>();
            final Map<Packfile, Set<Long>> seen = new HashMap<>();

            try (var ignored = task.split(1).begin("Add named entries")) {
                final Map<Long, List<Packfile>> packfiles = new HashMap<>();

                for (Packfile packfile : project.getPackfileManager().getArchives()) {
                    for (Packfile.FileEntry fileEntry : packfile.getFileEntries()) {
                        packfiles.computeIfAbsent(fileEntry.hash(), x -> new ArrayList<>()).add(packfile);
                    }
                }

                try (Stream<String> files = project.listAllFiles()) {
                    files.forEach(path -> {
                        final long hash = Packfile.getPathHash(path);
                        for (Packfile packfile : packfiles.getOrDefault(hash, Collections.emptyList())) {
                            final Packfile.FileEntry entry = Objects.requireNonNull(packfile.getFileEntry(hash));
                            info.add(new FileInfo(packfile, path, hash, entry.span().size()));
                            seen.computeIfAbsent(packfile, x -> new HashSet<>())
                                .add(hash);
                        }
                    });
                }
            }

            try (var ignored = task.split(1).begin("Add unnamed entries")) {
                for (Packfile packfile : project.getPackfileManager().getArchives()) {
                    final Set<Long> files = seen.get(packfile);
                    if (files == null) {
                        continue;
                    }
                    for (Packfile.FileEntry entry : packfile.getFileEntries()) {
                        final long hash = entry.hash();
                        if (files.contains(hash)) {
                            continue;
                        }
                        info.add(new FileInfo(packfile, "<unnamed>/%8x".formatted(hash), hash, entry.span().size()));
                        seen.computeIfAbsent(packfile, x -> new HashSet<>())
                            .add(hash);
                    }
                }
            }

            try (var ignored = task.split(1).begin("Compute file links")) {
                return new FileInfoIndex(info.toArray(FileInfo[]::new), project.listFileLinks());
            }
        }
    }

    private class FilterableTableModel extends AbstractTableModel {
        private static final FileInfo[] NO_RESULTS = new FileInfo[0];
        private static final int MAX_RESULTS = 1000;
        private static final MessageFormat STATUS_FORMAT = new MessageFormat("{0,choice,0#No results|1#{0} result|1<{0} results|" + MAX_RESULTS + "<{0} results (truncated)}");

        private final FileInfoIndex index;
        private FileInfo[] results;

        public FilterableTableModel(@NotNull FileInfoIndex index) {
            this.index = index;
            this.results = NO_RESULTS;
        }

        @Override
        public int getRowCount() {
            return results.length;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Packfile";
                case 1 -> "Path";
                case 2 -> "Size";
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final FileInfo info = results[rowIndex];

            return switch (columnIndex) {
                case 0 -> info.packfile.getName();
                case 1 -> info.path;
                case 2 -> IOUtils.formatSize(info.size);
                default -> null;
            };
        }

        @NotNull
        public FileInfo getValueAt(int rowIndex) {
            return results[rowIndex];
        }

        public void refresh(@NotNull String query, @NotNull Strategy strategy) {
            final int size = results.length;

            results = NO_RESULTS;
            statusBar.setVisible(false);

            if (size > 0) {
                fireTableRowsDeleted(0, size - 1);
            }

            if (!query.isEmpty()) {
                final long hash;
                final Matcher matcher = HASH_PATTERN.matcher(query);

                if (matcher.matches()) {
                    hash = Long.parseUnsignedLong(matcher.group(1), 16);
                } else {
                    hash = Packfile.getPathHash(Packfile.getNormalizedPath(query, false));
                }

                final FileInfo[] output = switch (strategy) {
                    case FIND_MATCHING -> Arrays.stream(index.files)
                        .filter(file -> file.hash == hash || file.path.contains(query))
                        .toArray(FileInfo[]::new);
                    case FIND_REFERENCED_BY -> Objects.requireNonNullElse(index.referencedBy.get(hash), NO_RESULTS);
                    case FIND_REFERENCES_TO -> Objects.requireNonNullElse(index.referencesTo.get(hash), NO_RESULTS);
                };

                statusBar.setVisible(true);

                if (output.length > 0) {
                    statusLabel.setText(STATUS_FORMAT.format(new Object[]{output.length}));
                    results = Arrays.copyOf(output, Math.min(output.length, MAX_RESULTS));
                    Arrays.sort(results, Comparator.comparing(FileInfo::packfile).thenComparing(FileInfo::path));
                    fireTableRowsInserted(0, results.length - 1);
                } else {
                    statusLabel.setText("No results");
                }
            }
        }
    }

    private class SearchHistoryAction extends AbstractAction {
        public SearchHistoryAction() {
            putValue(SMALL_ICON, new FlatSearchWithHistoryIcon(true));
            putValue(SHORT_DESCRIPTION, "Search History");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Deque<HistoryRecord> history = HISTORY.get(project);
            final JPopupMenu menu = new JPopupMenu();

            if (history != null && !history.isEmpty()) {
                for (HistoryRecord record : history) {
                    menu.add(new AbstractAction(record.query, record.strategy.icon) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            strategyCombo.setSelectedItem(record.strategy);
                            inputField.setText(record.query);
                            inputField.selectAll();
                        }
                    });
                }
            } else {
                menu.add(new EmptyAction("Empty"));
            }

            menu.show(inputField, strategyCombo.getWidth(), inputField.getHeight());
        }
    }

    private record HistoryRecord(@NotNull String query, @NotNull Strategy strategy) {}

    private record FileInfo(@NotNull Packfile packfile, @NotNull String path, long hash, int size) {}

    private static final class FileInfoIndex {
        private final FileInfo[] files;
        private final Map<Long, FileInfo[]> referencesTo;
        private final Map<Long, FileInfo[]> referencedBy;

        private FileInfoIndex(@NotNull FileInfo[] files, @NotNull Map<Long, long[]> links) {
            final Map<Long, List<FileInfo>> hashes = Arrays.stream(files).collect(Collectors.groupingBy(FileInfo::hash));
            final Map<Long, List<FileInfo>> referencesTo = new HashMap<>();
            final Map<Long, List<FileInfo>> referencedBy = new HashMap<>();

            for (Map.Entry<Long, long[]> entry : links.entrySet()) {
                final Long file = entry.getKey();

                for (long reference : entry.getValue()) {
                    referencesTo
                        .computeIfAbsent(reference, x -> new ArrayList<>())
                        .addAll(Objects.requireNonNullElseGet(hashes.get(file), List::of));

                    referencedBy
                        .computeIfAbsent(file, x -> new ArrayList<>())
                        .addAll(Objects.requireNonNullElseGet(hashes.get(reference), List::of));
                }
            }

            this.files = files;
            this.referencesTo = referencesTo.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().toArray(FileInfo[]::new)
            ));
            this.referencedBy = referencedBy.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().toArray(FileInfo[]::new)
            ));
        }
    }
}
