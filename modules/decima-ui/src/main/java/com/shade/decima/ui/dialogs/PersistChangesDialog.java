package com.shade.decima.ui.dialogs;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileWriter;
import com.shade.decima.model.packfile.PackfileWriter.Options;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.packfile.prefetch.PrefetchUpdater;
import com.shade.decima.model.packfile.resource.PackfileResource;
import com.shade.decima.model.util.FilePath;
import com.shade.decima.model.util.Oodle;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorFolderNode;
import com.shade.decima.ui.navigator.impl.NavigatorPackfilesNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;

public class PersistChangesDialog extends BaseDialog {
    private static final CompressionLevel[] COMPRESSION_LEVELS = {
        new CompressionLevel(Oodle.CompressionLevel.NONE, "None", "Don't compress"),
        new CompressionLevel(Oodle.CompressionLevel.SUPER_FAST, "Super Fast", "Super fast mode, lower compression ratio"),
        new CompressionLevel(Oodle.CompressionLevel.VERY_FAST, "Very Fast", "Fastest mode, decent compression ratio"),
        new CompressionLevel(Oodle.CompressionLevel.FAST, "Fast", "Good for daily use"),
        new CompressionLevel(Oodle.CompressionLevel.NORMAL, "Normal", "Standard medium speed mode"),
        new CompressionLevel(Oodle.CompressionLevel.OPTIMAL_1, "Optimal", "Faster optimal compression"),
        new CompressionLevel(Oodle.CompressionLevel.OPTIMAL_2, "Optimal 2", "Recommended baseline optimal encoder"),
        new CompressionLevel(Oodle.CompressionLevel.OPTIMAL_3, "Optimal 3", "Slower optimal encoder"),
        new CompressionLevel(Oodle.CompressionLevel.OPTIMAL_4, "Optimal 4", "Very slow optimal encoder"),
        new CompressionLevel(Oodle.CompressionLevel.OPTIMAL_5, "Optimal 5", "Maximum compression, VERY slow")
    };

    private static final PackfileType[] PACKFILE_TYPES = {
        new PackfileType("Regular", EnumSet.allOf(GameType.class)),
        new PackfileType("Encrypted", EnumSet.of(GameType.DS, GameType.DSDC)),
    };

    private static final Logger log = LoggerFactory.getLogger(PersistChangesDialog.class);

    private final NavigatorProjectNode root;
    private final JRadioButton updateExistingPackfileButton;
    private final JRadioButton createPatchPackfileButton;
    private final JCheckBox createBackupCheckbox;
    private final JCheckBox appendIfExistsCheckbox;
    private final JCheckBox rebuildPrefetchCheckbox;
    private final JCheckBox updateChangedFilesOnlyCheckbox;
    private final JComboBox<CompressionLevel> compressionLevelCombo;
    private final JComboBox<PackfileType> packfileTypeCombo;

    public PersistChangesDialog(@NotNull NavigatorProjectNode root) {
        super("Persist changes");

        this.root = root;

        this.updateExistingPackfileButton = Mnemonic.resolve(new JRadioButton("Update &changed packfiles", null, false));
        this.updateExistingPackfileButton.setToolTipText("""
            Repacks only that packfiles whose files were changed.
            Big packfiles might take a significant amount of time to repack.""");

        this.createPatchPackfileButton = Mnemonic.resolve(new JRadioButton("Collect changes into a &single packfile", null, true));
        this.createPatchPackfileButton.setToolTipText("""
            Creates a single packfile that contains all changes from modified packfiles.
            This option cannot be used when changing the same file across different packfiles.""");

        this.createBackupCheckbox = Mnemonic.resolve(new JCheckBox("Create &backup if exists", true));
        this.createBackupCheckbox.setToolTipText("Creates backup for every modified packfile so they can be restored later.");

        this.appendIfExistsCheckbox = Mnemonic.resolve(new JCheckBox("&Append if exists", true));
        this.appendIfExistsCheckbox.setToolTipText("If the selected packfile exists, appends changes rather than truncates it.");

        this.rebuildPrefetchCheckbox = Mnemonic.resolve(new JCheckBox("Rebuild &prefetch", true));
        this.rebuildPrefetchCheckbox.setToolTipText("""
            Rebuilds the prefetch file.
            The prefetch file contains a list of files and their references to other files that must be loaded when the game starts.
            This option must be used if one or more changed files are listed in the prefetch.""");

        this.updateChangedFilesOnlyCheckbox = new JCheckBox("Changed files only", true);
        this.updateChangedFilesOnlyCheckbox.setToolTipText("""
            Updates only those files that were changed.
            If disabled, all files listed in the prefetch will be updated.""");

        this.rebuildPrefetchCheckbox.addItemListener(e -> {
            final boolean selected = rebuildPrefetchCheckbox.isSelected();
            updateChangedFilesOnlyCheckbox.setEnabled(selected);
        });

        this.compressionLevelCombo = new JComboBox<>(COMPRESSION_LEVELS);
        this.compressionLevelCombo.setSelectedItem(COMPRESSION_LEVELS[3]);
        this.compressionLevelCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends CompressionLevel> list, @NotNull CompressionLevel value, int index, boolean selected, boolean focused) {
                append(value.name(), TextAttributes.REGULAR_ATTRIBUTES);

                if (value.description() != null) {
                    append(" " + value.description(), TextAttributes.GRAYED_SMALL_ATTRIBUTES);
                }
            }
        });

        this.packfileTypeCombo = new JComboBox<>(PACKFILE_TYPES);
        this.packfileTypeCombo.setSelectedItem(PACKFILE_TYPES[0]);
        this.packfileTypeCombo.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends PackfileType> list, @NotNull PackfileType value, int index, boolean selected, boolean focused) {
                append(value.name(), TextAttributes.REGULAR_ATTRIBUTES);

                if (!value.games().contains(root.getProjectContainer().getType())) {
                    append(" Incompatible with " + root.getProjectContainer().getType(), CommonTextAttributes.ERROR_ATTRIBUTES.smaller());
                }
            }
        });

        final ButtonGroup group = new ButtonGroup();
        group.add(updateExistingPackfileButton);
        group.add(createPatchPackfileButton);

        final boolean canMergeChanges = root.getProject().getPackfileManager().canMergeChanges();
        updateExistingPackfileButton.setSelected(!canMergeChanges);
        createPatchPackfileButton.setEnabled(canMergeChanges);
        createPatchPackfileButton.addItemListener(e -> appendIfExistsCheckbox.setEnabled(createPatchPackfileButton.isSelected()));
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JPanel settings = new JPanel();
        settings.setLayout(new MigLayout("ins panel", "[fill][grow,fill,250lp]", ""));
        settings.setBorder(new LabeledBorder("Settings"));

        {
            final JPanel top = new JPanel();
            top.setLayout(new MigLayout("ins 0", "[fill][fill]", ""));

            top.add(new JLabel("Strategy:"), "cell 0 0");
            top.add(updateExistingPackfileButton, "cell 0 1");
            top.add(createPatchPackfileButton, "cell 0 2");

            top.add(new JLabel("Options:"), "cell 1 0");
            top.add(createBackupCheckbox, "cell 1 1");
            top.add(appendIfExistsCheckbox, "cell 1 2");
            top.add(rebuildPrefetchCheckbox, "cell 1 3");
            top.add(updateChangedFilesOnlyCheckbox, "cell 1 4,gap ind");

            settings.add(top, "span");
        }

        final JLabel packfileTypeLabel = Mnemonic.resolve(new JLabel("Archive &format:"));
        packfileTypeLabel.setLabelFor(packfileTypeCombo);

        settings.add(packfileTypeLabel);
        settings.add(packfileTypeCombo, "wrap");

        final JLabel compressionLevelLabel = Mnemonic.resolve(new JLabel("Compression &level:"));
        compressionLevelLabel.setLabelFor(compressionLevelCombo);

        settings.add(compressionLevelLabel);
        settings.add(compressionLevelCombo, "wrap");

        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JScrollPane(createFilteredTree()), BorderLayout.CENTER);
        panel.add(settings, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        final Project project = root.getProject();

        if (descriptor == BUTTON_PERSIST) {
            final var update = updateExistingPackfileButton.isSelected();
            final var rebuildPrefetch = rebuildPrefetchCheckbox.isSelected();
            final var updateChangedFilesOnly = updateChangedFilesOnlyCheckbox.isSelected();
            final var compression = compressionLevelCombo.getItemAt(compressionLevelCombo.getSelectedIndex()).level();
            final var encrypt = packfileTypeCombo.getItemAt(packfileTypeCombo.getSelectedIndex()) == PACKFILE_TYPES[1];
            final var options = new PackfileWriter.Options(compression, encrypt);
            final Path outputPath;

            if (update) {
                final int result = JOptionPane.showConfirmDialog(
                    getDialog(),
                    "Updating modified packfiles can take a significant amount of time and render the game unplayable if important files were changed.\n\nAdditionally, to see the changes in the application, you might need to reload the project.\n\nDo you want to continue?",
                    "Confirm Update",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }

                outputPath = null;
            } else {
                final JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Choose output packfile");
                chooser.setFileFilter(new FileExtensionFilter("Decima packfile", "bin"));
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.setSelectedFile(project.getContainer().getPackfilesPath().resolve("Patch_New.bin").toFile());

                final int result = chooser.showSaveDialog(getDialog());

                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                outputPath = chooser.getSelectedFile().toPath();
            }

            final Optional<Boolean> result = ProgressDialog.showProgressDialog(getDialog(), "Persist changes", monitor -> {
                try (var task = monitor.begin("Persist changes", rebuildPrefetch ? 3 : 2)) {
                    final PrefetchUpdater.ChangeInfo prefetch;

                    if (rebuildPrefetch) {
                        prefetch = PrefetchUpdater.rebuildPrefetch(
                            task.split(1),
                            project,
                            updateChangedFilesOnly
                                ? PrefetchUpdater.FilePredicate.ofPackfileManager(project.getPackfileManager())
                                : PrefetchUpdater.FilePredicate.ofAll()
                        );
                    } else {
                        prefetch = null;
                    }

                    if (outputPath == null) {
                        updateExistingPackfiles(task.split(1), options, prefetch, createBackupCheckbox.isSelected());
                    } else {
                        collectSinglePackfile(task.split(1), outputPath, options, prefetch, appendIfExistsCheckbox.isSelected(), createBackupCheckbox.isSelected());
                    }

                    refreshPackfiles(task.split(1), project);
                } catch (IOException e) {
                    UIUtils.showErrorDialog(e, "Can't persist changes");
                    return false;
                }

                return true;
            });

            if (!result.orElse(false)) {
                return;
            }

            if (update) {
                JOptionPane.showMessageDialog(getDialog(), "Packfiles were updated successfully.");
            } else {
                JOptionPane.showMessageDialog(getDialog(), "Patch packfile was created successfully.");
            }
        }

        super.buttonPressed(descriptor);
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{BUTTON_PERSIST, BUTTON_CANCEL};
    }

    @Nullable
    @Override
    protected ButtonDescriptor getDefaultButton() {
        return BUTTON_PERSIST;
    }

    @NotNull
    private NavigatorTree createFilteredTree() {
        final NavigatorTree tree = new NavigatorTree(root);

        tree.getModel().setFilter(node -> {
            if (node instanceof NavigatorFolderNode n) {
                return n.getPackfile().hasChangesInPath(n.getPath());
            } else if (node instanceof NavigatorFileNode n) {
                return n.getPackfile().hasChangesInPath(n.getPath());
            } else if (node instanceof NavigatorPackfilesNode n) {
                return Arrays.stream(n.getPackfiles()).anyMatch(Packfile::hasChanges);
            } else {
                return false;
            }
        });

        tree.setRootVisible(false);

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

        return tree;
    }

    private void collectSinglePackfile(
        @NotNull ProgressMonitor monitor,
        @NotNull Path path,
        @NotNull Options options,
        @Nullable PrefetchUpdater.ChangeInfo prefetch,
        boolean append,
        boolean backup
    ) throws IOException {
        final Packfile packfile;

        if (append && Files.exists(path)) {
            packfile = new Packfile(path, root.getProject().getCompressor());
        } else {
            packfile = null;
        }

        final var project = root.getProject();
        final var manager = project.getPackfileManager();
        final var changes = manager.getPackfiles().stream()
            .filter(Packfile::hasChanges)
            .flatMap(p -> p.getChanges().entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (prefetch != null && changes.put(prefetch.path(), prefetch.change()) != null) {
            log.warn("Prefetch file is already in the list of changes and was overridden");
        }

        try (packfile) {
            write(monitor, path, packfile, options, changes, backup);
        }
    }

    private void updateExistingPackfiles(
        @NotNull ProgressMonitor monitor,
        @NotNull Options options,
        @Nullable PrefetchUpdater.ChangeInfo prefetch,
        boolean backup
    ) throws IOException {
        final var project = root.getProject();
        final var manager = project.getPackfileManager();
        final var changes = manager.getPackfiles().stream()
            .filter(Packfile::hasChanges)
            .collect(Collectors.toMap(
                Function.identity(),
                Packfile::getChanges
            ));

        if (prefetch != null && changes.computeIfAbsent(prefetch.packfile(), p -> new HashMap<>()).put(prefetch.path(), prefetch.change()) != null) {
            log.warn("Prefetch file is already in the list of changes and was overridden");
        }

        try (ProgressMonitor.Task task = monitor.begin("Update packfiles", changes.size())) {
            for (var changesPerPackfile : changes.entrySet()) {
                write(task.split(1), changesPerPackfile.getKey().getPath(), changesPerPackfile.getKey(), options, changesPerPackfile.getValue(), backup);
            }
        }
    }

    private void write(@NotNull ProgressMonitor monitor, @NotNull Path path, @Nullable Packfile target, @NotNull Options options, @NotNull Map<FilePath, Change> changes, boolean backup) throws IOException {
        try (ProgressMonitor.Task task = monitor.begin("Build packfile", 1)) {
            try (PackfileWriter writer = new PackfileWriter()) {
                if (target != null) {
                    final Set<Long> hashes = changes.keySet().stream()
                        .map(FilePath::hash)
                        .collect(Collectors.toSet());

                    for (PackfileBase.FileEntry file : target.getFileEntries()) {
                        if (!hashes.contains(file.hash())) {
                            writer.add(new PackfileResource(target, file));
                        }
                    }
                }

                for (Change change : changes.values()) {
                    writer.add(change.toResource());
                }

                final Path result = Path.of(path + ".tmp");

                try (FileChannel channel = FileChannel.open(result, WRITE, CREATE, TRUNCATE_EXISTING)) {
                    writer.write(task.split(1), channel, root.getProject().getCompressor(), options);
                }

                if (task.isCanceled()) {
                    Files.deleteIfExists(result);
                    return;
                }

                if (backup && Files.exists(path)) {
                    try {
                        Files.move(path, IOUtils.makeBackupPath(path));
                    } catch (IOException e) {
                        UIUtils.showErrorDialog(e, "Unable to create backup");
                    }
                }

                Files.move(result, path, REPLACE_EXISTING);
            }
        }
    }

    private static void refreshPackfiles(@NotNull ProgressMonitor monitor, @NotNull Project project) {
        try (var task = monitor.begin("Refresh packfiles")) {
            for (Packfile packfile : project.getPackfileManager().getPackfiles()) {
                if (task.isCanceled()) {
                    return;
                }

                if (packfile.hasChanges()) {
                    try {
                        packfile.reload(true);
                    } catch (IOException e) {
                        UIUtils.showErrorDialog(e, "Unable to reload packfile");
                    }
                }
            }
        }
    }

    private record CompressionLevel(@NotNull Oodle.CompressionLevel level, @NotNull String name, @Nullable String description) {}

    private record PackfileType(@NotNull String name, EnumSet<GameType> games) {}
}
