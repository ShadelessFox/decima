package com.shade.decima.ui.dialogs;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileWriter;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.packfile.resource.PackfileResource;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.FilePath;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorFolderNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.ProgressMonitor;
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

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

public class PersistChangesDialog extends BaseDialog {
    private static final CompressionLevel[] COMPRESSION_LEVELS = {
        new CompressionLevel(Compressor.Level.NONE, "None", "Don't compress"),
        new CompressionLevel(Compressor.Level.SUPER_FAST, "Super Fast", "Super fast mode, lower compression ratio"),
        new CompressionLevel(Compressor.Level.VERY_FAST, "Very Fast", "Fastest mode, decent compression ratio"),
        new CompressionLevel(Compressor.Level.FAST, "Fast", "Good for daily use"),
        new CompressionLevel(Compressor.Level.NORMAL, "Normal", "Standard medium speed mode"),
        new CompressionLevel(Compressor.Level.OPTIMAL_1, "Optimal", "Faster optimal compression"),
        new CompressionLevel(Compressor.Level.OPTIMAL_2, "Optimal 2", "Recommended baseline optimal encoder"),
        new CompressionLevel(Compressor.Level.OPTIMAL_3, "Optimal 3", "Slower optimal encoder"),
        new CompressionLevel(Compressor.Level.OPTIMAL_4, "Optimal 4", "Very slow optimal encoder"),
        new CompressionLevel(Compressor.Level.OPTIMAL_5, "Optimal 5", "Maximum compression, VERY slow")
    };

    private static final PackfileType[] PACKFILE_TYPES = {
        new PackfileType("Regular", EnumSet.allOf(GameType.class)),
        new PackfileType("Encrypted", EnumSet.of(GameType.DS)),
    };

    private final NavigatorProjectNode root;
    private final JRadioButton updateExistingPackfileButton;
    private final JRadioButton createPatchPackfileButton;
    private final JComboBox<CompressionLevel> compressionLevelCombo;
    private final JComboBox<PackfileType> packfileTypeCombo;

    public PersistChangesDialog(@NotNull NavigatorProjectNode root) {
        super("Persist changes", List.of(BUTTON_PERSIST, BUTTON_CANCEL));

        this.root = root;

        this.updateExistingPackfileButton = Mnemonic.resolve(new JRadioButton("Update &changed packfiles", null, false));
        this.updateExistingPackfileButton.setToolTipText("Repacks only that packfiles whose files were changed.\nBig packfiles might take a significant amount of time to repack.");

        this.createPatchPackfileButton = Mnemonic.resolve(new JRadioButton("Collect changes into a &single packfile", null, true));
        this.createPatchPackfileButton.setToolTipText("Creates a single packfile that contains all changes from modified packfiles.\nThis option cannot be used when changing the same file across different packfiles.");

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
                    append(" Incompatible with " + root.getProjectContainer().getType(), CommonTextAttributes.IDENTIFIER_ATTRIBUTES.smaller());
                }
            }
        });

        final ButtonGroup group = new ButtonGroup();
        group.add(updateExistingPackfileButton);
        group.add(createPatchPackfileButton);

        final boolean canMergeChanges = root.getProject().getPackfileManager().canMergeChanges();
        updateExistingPackfileButton.setSelected(!canMergeChanges);
        createPatchPackfileButton.setEnabled(canMergeChanges);
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JPanel options = new JPanel();
        options.setLayout(new MigLayout("ins panel", "[fill][grow,fill,250lp]", ""));
        options.setBorder(new LabeledBorder(new JLabel("Options")));

        options.add(new JLabel("Update strategy:"), "wrap");
        options.add(updateExistingPackfileButton, "x ind,span");
        options.add(createPatchPackfileButton, "x ind,span");

        final JLabel packfileTypeLabel = Mnemonic.resolve(new JLabel("Archive &format:"));
        packfileTypeLabel.setLabelFor(packfileTypeCombo);

        options.add(packfileTypeLabel);
        options.add(packfileTypeCombo, "wrap");

        final JLabel compressionLevelLabel = Mnemonic.resolve(new JLabel("Compression &level:"));
        compressionLevelLabel.setLabelFor(compressionLevelCombo);

        options.add(compressionLevelLabel);
        options.add(compressionLevelCombo, "wrap");

        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JScrollPane(createFilteredTree()), BorderLayout.CENTER);
        panel.add(options, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        if (descriptor == BUTTON_PERSIST) {
            final var update = updateExistingPackfileButton.isSelected();
            final var compression = compressionLevelCombo.getItemAt(compressionLevelCombo.getSelectedIndex()).level();
            final var encrypt = packfileTypeCombo.getItemAt(packfileTypeCombo.getSelectedIndex()) == PACKFILE_TYPES[1];
            final boolean success;

            try {
                success = persist(update, new PackfileWriter.Options(compression, encrypt));
            } catch (IOException e) {
                throw new RuntimeException("Error persisting changes", e);
            }

            if (success) {
                root.getProject().getPackfileManager().clearChanges();
            } else {
                return;
            }
        }

        super.buttonPressed(descriptor);
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
                return n.getProject().getPackfileManager().hasChangesInPath(n.getPackfile(), n.getPath());
            }
            if (node instanceof NavigatorFileNode n) {
                return n.getProject().getPackfileManager().hasChangesInPath(n.getPackfile(), n.getPath());
            }
            return false;
        });

        tree.setRootVisible(false);

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

        return tree;
    }

    @NotNull
    private boolean persist(boolean update, @NotNull PackfileWriter.Options options) throws IOException {
        if (update) {
            final int result = JOptionPane.showConfirmDialog(
                getDialog(),
                "Updating modified packfiles can take a significant amount of time and render the game unplayable if important files were changed.\n\nAdditionally, to see the changes in the application, you might need to reload the project.\n\nDo you want to continue?",
                "Confirm Update",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                final int createBackupsResult = JOptionPane.showConfirmDialog(
                    getDialog(),
                    "Would you like to keep original packfiles as an emergency backup?",
                    "Confirm Backup",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );

                ProgressDialog.showProgressDialog(getDialog(), "Persist changes", monitor -> {
                    updateExistingPackfiles(monitor, options, createBackupsResult == JOptionPane.OK_OPTION);
                    return null;
                });

                JOptionPane.showMessageDialog(getDialog(), "Packfiles were updated successfully.");

                return true;
            }
        } else {
            final JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose output packfile");
            chooser.setFileFilter(new FileExtensionFilter("Decima packfile", "bin"));
            chooser.setAcceptAllFileFilterUsed(false);

            final int result = chooser.showSaveDialog(getDialog());

            if (result == JFileChooser.APPROVE_OPTION) {
                final int truncateOutputResult = JOptionPane.showConfirmDialog(
                    Application.getFrame(),
                    "The selected file exists. Would you like to truncate it before continuing?",
                    "Confirm Truncate",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );

                if (truncateOutputResult == JOptionPane.YES_OPTION || truncateOutputResult == JOptionPane.NO_OPTION) {
                    ProgressDialog.showProgressDialog(getDialog(), "Persist changes", monitor -> {
                        persistAsPatch(monitor, chooser.getSelectedFile().toPath(), options, truncateOutputResult == JOptionPane.NO_OPTION, true);
                        return null;
                    });

                    JOptionPane.showMessageDialog(getDialog(), "Patch packfile was created successfully.");

                    return true;
                }
            }
        }

        return false;
    }

    private void persistAsPatch(@NotNull ProgressMonitor monitor, @NotNull Path path, @NotNull PackfileWriter.Options options, boolean append, boolean backup) throws IOException {
        final var project = root.getProject();
        final var manager = project.getPackfileManager();
        final var changes = manager.getMergedChanges();

        try (ProgressMonitor.Task task = monitor.begin("Create patch packfile", 1)) {
            final Packfile packfile;

            if (append) {
                packfile = new Packfile(Files.newByteChannel(path), project.getCompressor(), null, path);
            } else {
                packfile = null;
            }

            try (PackfileWriter writer = new PackfileWriter(); packfile) {
                for (Map<FilePath, Change> changesPerPackfile : changes.values()) {
                    final var changeForPath = changesPerPackfile.entrySet().stream()
                        .collect(Collectors.toMap(
                            x -> x.getKey().hash(),
                            Map.Entry::getValue
                        ));

                    if (append) {
                        for (PackfileBase.FileEntry file : packfile.getFileEntries()) {
                            if (!changeForPath.containsKey(file.hash())) {
                                writer.add(new PackfileResource(packfile, file));
                            }
                        }
                    }

                    for (Change change : changesPerPackfile.values()) {
                        writer.add(change.toResource());
                    }
                }

                final Path result = Path.of(path + ".tmp");

                try (FileChannel channel = FileChannel.open(result, WRITE, CREATE, TRUNCATE_EXISTING)) {
                    writer.write(monitor, channel, project.getCompressor(), options);
                }

                if (backup) {
                    try {
                        Files.move(path, makeBackupPath(path));
                    } catch (IOException e) {
                        UIUtils.showErrorDialog(Application.getFrame(), e, "Unable to create backup");
                    }
                }

                Files.move(result, path);
            }

            task.worked(1);
        }
    }

    private void updateExistingPackfiles(@NotNull ProgressMonitor monitor, @NotNull PackfileWriter.Options options, boolean backup) throws IOException {
        final var project = root.getProject();
        final var manager = project.getPackfileManager();
        final var changes = manager.getMergedChanges();

        try (ProgressMonitor.Task task = monitor.begin("Update packfiles", changes.size())) {
            for (var changesPerPackfile : changes.entrySet()) {
                final var packfile = changesPerPackfile.getKey();
                final var changeForPath = changesPerPackfile.getValue().entrySet().stream()
                    .collect(Collectors.toMap(
                        x -> x.getKey().hash(),
                        Map.Entry::getValue
                    ));

                try (PackfileWriter writer = new PackfileWriter()) {
                    for (PackfileBase.FileEntry file : packfile.getFileEntries()) {
                        if (!changeForPath.containsKey(file.hash())) {
                            writer.add(new PackfileResource(packfile, file));
                        }
                    }

                    for (Change change : changeForPath.values()) {
                        writer.add(change.toResource());
                    }

                    final Path result = Path.of(packfile.getPath() + ".tmp");

                    try (FileChannel channel = FileChannel.open(result, WRITE, CREATE, TRUNCATE_EXISTING)) {
                        writer.write(monitor, channel, project.getCompressor(), options);
                    }

                    if (backup) {
                        try {
                            Files.move(packfile.getPath(), makeBackupPath(packfile.getPath()));
                        } catch (IOException e) {
                            UIUtils.showErrorDialog(Application.getFrame(), e, "Unable to create backup");
                        }
                    }

                    Files.move(result, packfile.getPath());
                }

                task.worked(1);
            }
        }
    }

    @NotNull
    private Path makeBackupPath(@NotNull Path path) {
        for (int suffix = 0; ; suffix++) {
            final Path result;

            if (suffix == 0) {
                result = Path.of(path + ".bak");
            } else {
                result = Path.of(path + ".bak" + suffix);
            }

            if (Files.notExists(result)) {
                return result;
            }
        }
    }

    private record CompressionLevel(@NotNull Compressor.Level level, @NotNull String name, @Nullable String description) {}

    private record PackfileType(@NotNull String name, EnumSet<GameType> games) {}
}
