package com.shade.decima.ui.dialogs;

import com.formdev.flatlaf.util.SystemInfo;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.util.Oodle;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.validators.ExistingFileValidator;
import com.shade.decima.ui.controls.validators.NotEmptyValidator;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.DocumentAdapter;
import com.shade.platform.ui.controls.LabeledSeparator;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.dialogs.BaseEditDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public class ProjectEditDialog extends BaseEditDialog {
    private final boolean edit;

    private final JTextField projectUuid;
    private final JTextField projectName;
    private final JComboBox<GameType> projectType;
    private final JTextField executableFilePath;
    private final JTextField archiveFolderPath;
    private final JTextField compressorPath;
    private final ColoredComponent compressorNote;
    private final JTextField rttiInfoFilePath;
    private final JTextField fileListingsPath;

    public ProjectEditDialog(boolean edit) {
        super(edit ? "Edit Project" : "New Project");

        this.edit = edit;

        this.projectUuid = new JTextField();
        this.projectUuid.setEditable(false);
        this.projectName = new JTextField();
        this.projectType = new JComboBox<>(GameType.values());
        this.executableFilePath = new JTextField();
        this.archiveFolderPath = new JTextField();
        this.compressorPath = new JTextField();
        this.compressorNote = new ColoredComponent();
        this.rttiInfoFilePath = new JTextField();
        this.fileListingsPath = new JTextField();

        this.compressorNote.setVisible(false);
        this.compressorPath.getDocument().addDocumentListener((DocumentAdapter) e -> {
            if (UIUtils.isValid(compressorPath)) {
                compressorNote.clear();

                try (Oodle oodle = Oodle.acquire(Path.of(compressorPath.getText()))) {
                    compressorNote.append("Oodle library version: " + oodle.getVersionString(), TextAttributes.GRAYED_SMALL_ATTRIBUTES);
                } catch (Throwable ex) {
                    compressorNote.append("Can't detect Oodle library version. Your PC might explode!", TextAttributes.GRAYED_SMALL_ATTRIBUTES);
                }

                compressorNote.setVisible(true);
            } else {
                compressorNote.setVisible(false);
            }

            fitContent();
        });

        if (!edit) {
            projectType.addItemListener(e -> fillValuesBasedOnGameType((GameType) e.getItem(), projectType.getItemAt(projectType.getSelectedIndex())));

            executableFilePath.getDocument().addDocumentListener((DocumentAdapter) e -> {
                if (UIUtils.isValid(executableFilePath)) {
                    fillValuesBasedOnGameExecutable(Path.of(executableFilePath.getText()));
                }
            });
        }
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("insets 0", "[fill][grow,fill,400lp]", ""));

        panel.add(new LabeledSeparator("Project"), "span,wrap");

        if (edit) {
            panel.add(new JLabel("UUID:"), "gap ind");
            panel.add(projectUuid, "wrap");
        }

        {
            panel.add(new JLabel("Name:"), "gap ind");
            panel.add(projectName, "wrap");

            UIUtils.installInputValidator(projectName, new NotEmptyValidator(projectName), this);
        }

        {
            panel.add(new JLabel("Type:"), "gap ind");
            panel.add(projectType, "wrap");
        }

        panel.add(new LabeledSeparator("Game"), "span,wrap");

        {
            final FileExtensionFilter filter = new FileExtensionFilter("Game Executable", "exe");

            final JLabel label = new JLabel("Executable file:");
            label.setToolTipText("<html>Path to the game's binary executable.<br>For most games, it's the only <kbd>.exe</kbd> file located in the game's root folder.</html>");

            panel.add(label, "gap ind");
            panel.add(executableFilePath, "wrap");

            UIUtils.addOpenFileAction(executableFilePath, "Select game executable", filter);
            UIUtils.installInputValidator(executableFilePath, new ExistingFileValidator(executableFilePath, filter), this);
        }

        {
            final JLabel label = new JLabel("Data directory:");
            label.setToolTipText("<html>Path to the game's archives folder.<br>For most games, it's a folder in the game's root folder that contains a bunch of <kbd>.bin</kbd> files.</html>");

            panel.add(label, "gap ind");
            panel.add(archiveFolderPath, "wrap");

            UIUtils.addOpenDirectoryAction(archiveFolderPath, "Select folder containing game archives");
            UIUtils.installInputValidator(archiveFolderPath, new ExistingFileValidator(archiveFolderPath, null), this);
        }

        {
            final String extension = SystemInfo.isMacOS ? "dylib" : SystemInfo.isLinux ? "so" : "dll";
            final FileExtensionFilter filter = new FileExtensionFilter("Oodle Library", extension);

            final JLabel label = new JLabel("Oodle library:");
            label.setToolTipText("<html>Path to the oodle library used for compressing/decompressing game data.<br>For most games, it's a file in the game's root folder called <kbd>oo2core_XXX." + extension + "</kbd>.</html>");

            panel.add(label, "gap ind");
            panel.add(compressorPath, "wrap");
            panel.add(compressorNote, "hidemode 2,skip,wrap");

            UIUtils.addOpenFileAction(compressorPath, "Select Oodle library", filter);
            UIUtils.installInputValidator(compressorPath, new ExistingFileValidator(compressorPath, filter), this);
        }

        panel.add(new LabeledSeparator("Metadata"), "span,wrap");

        {
            final FileExtensionFilter filter = new FileExtensionFilter("RTTI information", "json", "json.gz");

            final JLabel label = new JLabel("Type information:");
            label.setToolTipText("Path to a file containing information about all data types found in game files.");

            panel.add(label, "gap ind");
            panel.add(rttiInfoFilePath, "wrap");

            UIUtils.addOpenFileAction(rttiInfoFilePath, "Select RTTI information file", filter);
            UIUtils.installInputValidator(rttiInfoFilePath, new ExistingFileValidator(rttiInfoFilePath, filter), this);
        }

        {
            final FileExtensionFilter filter = new FileExtensionFilter("File listings", "txt", "txt.gz");

            final JLabel label = new JLabel("File listings:");
            label.setToolTipText("<html>Path to a file containing information about the complete list of files.<br>This file is not required, but all projects will benefit from it as it includes files that can't be normally seen<br>under their original names (instead, you would see a bunch of files under the <kbd>&lt;unnamed&gt;</kbd> folder in the navigator tree)</html>");

            panel.add(label, "gap ind");
            panel.add(fileListingsPath, "wrap");

            UIUtils.addOpenFileAction(fileListingsPath, "Select file containing file listings", filter);
            UIUtils.installInputValidator(fileListingsPath, new ExistingFileValidator(fileListingsPath, filter, false), this);
        }

        if (!edit) {
            fillValuesBasedOnGameType(projectType.getItemAt(0), projectType.getItemAt(0));
        }

        return panel;
    }

    @Nullable
    @Override
    protected JComponent getDefaultComponent() {
        return projectName;
    }

    public void load(@NotNull ProjectContainer container) {
        projectUuid.setText(container.getId().toString());
        projectName.setText(container.getName());
        projectType.setSelectedItem(container.getType());
        executableFilePath.setText(container.getExecutablePath().toString());
        archiveFolderPath.setText(container.getPackfilesPath().toString());
        compressorPath.setText(container.getCompressorPath().toString());
        rttiInfoFilePath.setText(container.getTypeMetadataPath().toString());
        fileListingsPath.setText(container.getFileListingsPath() == null ? null : container.getFileListingsPath().toString());
    }

    public void save(@NotNull ProjectContainer container) {
        container.setName(projectName.getText());
        container.setType((GameType) Objects.requireNonNull(projectType.getSelectedItem()));
        container.setExecutablePath(Path.of(executableFilePath.getText()));
        container.setPackfilesPath(Path.of(archiveFolderPath.getText()));
        container.setCompressorPath(Path.of(compressorPath.getText()));
        container.setTypeMetadataPath(Path.of(rttiInfoFilePath.getText()));
        container.setFileListingsPath(fileListingsPath.getText().isEmpty() ? null : Path.of(fileListingsPath.getText()));
    }

    @Override
    public boolean isComplete() {
        return UIUtils.isValid(projectName)
            && UIUtils.isValid(executableFilePath)
            && UIUtils.isValid(archiveFolderPath)
            && UIUtils.isValid(rttiInfoFilePath)
            && UIUtils.isValid(compressorPath);
    }

    private void fillValuesBasedOnGameType(@NotNull GameType oldType, @NotNull GameType newType) {
        final KnownValues oldValues = KnownValues.of(oldType);
        final KnownValues newValues = KnownValues.of(newType);

        setIfEmptyOrOldValue(rttiInfoFilePath, oldValues.rttiInfo, newValues.rttiInfo);
        setIfEmptyOrOldValue(fileListingsPath, oldValues.fileListings, newValues.fileListings);
    }

    private void fillValuesBasedOnGameExecutable(@NotNull Path path) {
        final String newFilename = IOUtils.getBasename(path.getFileName().toString().toLowerCase(Locale.ROOT));
        final String libExtension = SystemInfo.isMacOS ? "dylib" : SystemInfo.isLinux ? "so" : "dll";

        switch (newFilename) {
            case "ds" -> {
                setIfEmptyOrOldValue(archiveFolderPath, Path.of(archiveFolderPath.getText()), path.resolveSibling("data"));
                setIfEmptyOrOldValue(compressorPath, Path.of(compressorPath.getText()), path.resolveSibling("oo2core_7_win64." + libExtension));
            }
            case "horizonzerodawn" -> {
                setIfEmptyOrOldValue(archiveFolderPath, Path.of(archiveFolderPath.getText()), path.resolveSibling("Packed_DX12"));
                setIfEmptyOrOldValue(compressorPath, Path.of(compressorPath.getText()), path.resolveSibling("oo2core_3_win64." + libExtension));
            }
        }
    }

    private static void setIfEmptyOrOldValue(@NotNull JTextComponent component, @NotNull Path oldPath, @NotNull Path newPath) {
        setIfEmptyOrOldValue(component, oldPath.toAbsolutePath().toString(), newPath.toAbsolutePath().toString());
    }

    private static void setIfEmptyOrOldValue(@NotNull JTextComponent component, @NotNull String oldText, @NotNull String newText) {
        final String trimmed = component.getText().trim();

        if (!trimmed.isEmpty() && !trimmed.equals(oldText)) {
            return;
        }

        component.setText(newText);
    }

    // We do know that these files exist, and we may use this information wisely.
    // I'd like to embed these files in resources and choose based on the selected
    // game type, but it might be useful to be able to specify custom values for these
    private record KnownValues(@NotNull Path rttiInfo, @NotNull Path archiveInfo, @NotNull Path fileListings) {
        @NotNull
        public static KnownValues of(@NotNull GameType type) {
            return switch (type) {
                case DS -> new KnownValues(
                    Path.of("data/ds_types.json.gz"),
                    Path.of("data/ds_archives.json.gz"),
                    Path.of("data/ds_paths.txt.gz")
                );
                case DSDC -> new KnownValues(
                    Path.of("data/dsdc_types.json.gz"),
                    Path.of("data/dsdc_archives.json.gz"),
                    Path.of("data/dsdc_paths.txt.gz")
                );
                case HZD -> new KnownValues(
                    Path.of("data/hzd_types.json.gz"),
                    Path.of("data/hzd_archives.json.gz"),
                    Path.of("data/hzd_paths.txt.gz")
                );
            };
        }
    }
}
