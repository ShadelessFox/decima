package com.shade.decima.ui.dialogs;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectPersister;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.PackfileWriter;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.controls.ColoredListCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.dialogs.BaseDialog;
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
        this.updateExistingPackfileButton = new JRadioButton("Update existing packfiles");
        this.createPatchPackfileButton = new JRadioButton("Create patch packfile", true);

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

        // TODO: Not implemented
        updateExistingPackfileButton.setEnabled(false);
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
        panel.add(new JScrollPane(tree), BorderLayout.CENTER);
        panel.add(options, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        if (descriptor == BUTTON_PERSIST) {
            if (updateExistingPackfileButton.isSelected()) {
                final int result = JOptionPane.showConfirmDialog(
                    getDialog(),
                    "Updating existing packfiles can take a significant amount of time and render the game unplayable if important files were changed.\n\nDo you want to continue?",
                    "Confirm packfile update",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.ERROR_MESSAGE);

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            } else {
                final JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Choose output packfile");
                chooser.setFileFilter(new FileExtensionFilter("Decima packfile", "bin"));
                chooser.setAcceptAllFileFilterUsed(false);

                final int result = chooser.showSaveDialog(getDialog());

                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                final PackfileWriter.Options options = new PackfileWriter.Options(
                    compressionLevelCombo.getItemAt(compressionLevelCombo.getSelectedIndex()).level(),
                    packfileTypeCombo.getItemAt(packfileTypeCombo.getSelectedIndex()) == PACKFILE_TYPES[1]
                );

                try {
                    persistAsPatch(new VoidProgressMonitor(), chooser.getSelectedFile().toPath(), options);
                } catch (IOException e) {
                    throw new RuntimeException("Error writing patch packfile", e);
                }
            }
        }

        super.buttonPressed(descriptor);
    }

    @Nullable
    @Override
    protected ButtonDescriptor getDefaultButton() {
        return BUTTON_PERSIST;
    }

    private void persistAsPatch(@NotNull ProgressMonitor monitor, @NotNull Path path, @NotNull PackfileWriter.Options options) throws IOException {
        final Project project = root.getProject();
        final ProjectPersister persister = project.getPersister();

        try (PackfileWriter writer = new PackfileWriter()) {
            for (NavigatorFileNode file : persister.getFiles()) {
                writer.add(persister.getMergedChange(file).toResource());
            }

            try (FileChannel channel = FileChannel.open(path, WRITE, CREATE, TRUNCATE_EXISTING)) {
                writer.write(monitor, channel, project.getCompressor(), options);
            }
        }

        final TreeModel model = Application.getFrame().getNavigator().getModel();
        final TreeNode[] nodes = persister.getFiles().toArray(TreeNode[]::new);

        persister.clearChanges();

        for (TreeNode node : nodes) {
            model.fireNodesChanged(node);
        }

        JOptionPane.showMessageDialog(getDialog(), "Patch packfile was created successfully.");
    }

    private static record CompressionLevel(@NotNull Compressor.Level level, @NotNull String name, @Nullable String description) {}

    private static record PackfileType(@NotNull String name, EnumSet<GameType> games) {}
}
