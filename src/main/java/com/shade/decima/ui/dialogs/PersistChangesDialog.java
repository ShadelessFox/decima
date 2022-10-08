package com.shade.decima.ui.dialogs;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectPersister;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.packfile.PackfileWriter;
import com.shade.decima.model.packfile.resource.PackfileResource;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        this.updateExistingPackfileButton = new JRadioButton(null, null, false);
        this.createPatchPackfileButton = new JRadioButton(null, null, true);

        Mnemonic.apply("Update &existing packfiles", updateExistingPackfileButton);
        Mnemonic.apply("Create &patch packfile", createPatchPackfileButton);

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
                    append(" Incompatible with " + root.getProjectContainer().getType(), TextAttributes.DARK_RED_ATTRIBUTES.smaller());
                }
            }
        });

        final ButtonGroup group = new ButtonGroup();
        group.add(updateExistingPackfileButton);
        group.add(createPatchPackfileButton);
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final NavigatorTree tree = new NavigatorTree(root);
        tree.getModel().setFilter(node -> {
            final NavigatorProjectNode parent = node.findParentOfType(NavigatorProjectNode.class);
            return parent != null && !parent.needsInitialization()
                && parent.getProject().getPersister().hasChangesInPath(node);
        });
        tree.setRootVisible(false);

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

        final JPanel options = new JPanel();
        options.setLayout(new MigLayout("ins panel", "[fill][grow,fill,250lp]", ""));
        options.setBorder(new LabeledBorder(new JLabel("Options")));

        options.add(new JLabel("Update strategy:"), "wrap");
        options.add(updateExistingPackfileButton, "x ind,wrap");
        options.add(createPatchPackfileButton, "x ind,wrap");

        options.add(new JLabel("Compression level:"));
        options.add(compressionLevelCombo, "wrap");

        options.add(new JLabel("Packfile version:"));
        options.add(packfileTypeCombo, "wrap");

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

            if (!success) {
                return;
            }

            final ProjectPersister persister = root.getProject().getPersister();
            final TreeModel model = Application.getFrame().getNavigator().getModel();
            final TreeNode[] nodes = persister.getFiles().toArray(TreeNode[]::new);

            persister.clearChanges();

            for (TreeNode node : nodes) {
                model.fireNodesChanged(node);
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
            final NavigatorProjectNode parent = node.findParentOfType(NavigatorProjectNode.class);
            return parent != null && !parent.needsInitialization() && parent.getProject().getPersister().hasChangesInPath(node);
        });

        tree.setRootVisible(false);

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

        return tree;
    }

    private boolean persist(boolean update, @NotNull PackfileWriter.Options options) throws IOException {
        if (update) {
            final int result = JOptionPane.showConfirmDialog(
                getDialog(),
                "Updating existing packfiles can take a significant amount of time and render the game unplayable if important files were changed.\n\nDo you want to continue?",
                "Confirm Update",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                ProgressDialog.showProgressDialog(getDialog(), "Persist changes", monitor -> {
                    updateExistingPackfiles(monitor, options);
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
                ProgressDialog.showProgressDialog(getDialog(), "Persist changes", monitor -> {
                    persistAsPatch(monitor, chooser.getSelectedFile().toPath(), options);
                    return null;
                });

                JOptionPane.showMessageDialog(getDialog(), "Patch packfile was created successfully.");

                return true;
            }
        }

        return false;
    }

    private void persistAsPatch(@NotNull ProgressMonitor monitor, @NotNull Path path, @NotNull PackfileWriter.Options options) throws IOException {
        final Project project = root.getProject();
        final ProjectPersister persister = project.getPersister();

        try (ProgressMonitor.Task task = monitor.begin("Create patch packfile", 1)) {
            try (PackfileWriter writer = new PackfileWriter()) {
                for (NavigatorFileNode file : persister.getFiles()) {
                    writer.add(persister.getMergedChange(file).toResource());
                }

                try (FileChannel channel = FileChannel.open(path, WRITE, CREATE, TRUNCATE_EXISTING)) {
                    writer.write(monitor, channel, project.getCompressor(), options);
                }
            }

            task.worked(1);
        }
    }

    private void updateExistingPackfiles(@NotNull ProgressMonitor monitor, @NotNull PackfileWriter.Options options) throws IOException {
        final var project = root.getProject();
        final var persister = project.getPersister();
        final var groups = persister.getFiles().stream()
            .collect(Collectors.groupingBy(
                NavigatorNode::getPackfile,
                Collectors.mapping(NavigatorFileNode::getHash, Collectors.toSet())
            ));

        try (ProgressMonitor.Task task = monitor.begin("Update packfiles", groups.size())) {
            for (Map.Entry<Packfile, Set<Long>> entry : groups.entrySet()) {
                final Packfile packfile = entry.getKey();
                final Set<Long> changes = entry.getValue();

                try (PackfileWriter writer = new PackfileWriter()) {
                    for (PackfileBase.FileEntry file : packfile.getFileEntries()) {
                        if (!changes.contains(file.hash())) {
                            writer.add(new PackfileResource(packfile, file));
                        }
                    }

                    for (NavigatorFileNode file : persister.getFiles()) {
                        writer.add(persister.getMergedChange(file).toResource());
                    }

                    try (FileChannel channel = FileChannel.open(Path.of(packfile.getPath() + ".patch"), WRITE, CREATE, TRUNCATE_EXISTING)) {
                        writer.write(monitor, channel, project.getCompressor(), options);
                    }
                }

                task.worked(1);
            }
        }
    }

    private static record CompressionLevel(@NotNull Compressor.Level level, @NotNull String name, @Nullable String description) {}

    private static record PackfileType(@NotNull String name, EnumSet<GameType> games) {}
}
