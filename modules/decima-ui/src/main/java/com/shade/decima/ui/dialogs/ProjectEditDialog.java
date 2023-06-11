package com.shade.decima.ui.dialogs;

import com.formdev.flatlaf.util.SystemInfo;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.GameType;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledSeparator;
import com.shade.decima.ui.controls.validators.ExistingFileValidator;
import com.shade.decima.ui.controls.validators.NotEmptyValidator;
import com.shade.platform.ui.dialogs.BaseEditDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Objects;

public class ProjectEditDialog extends BaseEditDialog {
    private final boolean edit;

    private final JTextField projectUuid;
    private final JTextField projectName;
    private final JComboBox<GameType> projectType;
    private final JTextField executableFilePath;
    private final JTextField archiveFolderPath;
    private final JTextField compressorPath;
    private final JTextField rttiInfoFilePath;
    private final JTextField archiveInfoFilePath;
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
        this.rttiInfoFilePath = new JTextField();
        this.archiveInfoFilePath = new JTextField();
        this.fileListingsPath = new JTextField();
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

            final JLabel label = new JLabel("Compressor library:");
            label.setToolTipText("<html>Path to the compressor library used for compressing/decompressing game data.<br>For most games, it's a file in the game's root folder called <kbd>oo2core_XXX." + extension + "</kbd>.</html>");

            panel.add(label, "gap ind");
            panel.add(compressorPath, "wrap");

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
            final FileExtensionFilter filter = new FileExtensionFilter("Archive information", "json", "json.gz");

            final JLabel label = new JLabel("Archive information:");
            label.setToolTipText("<html>Path to a file containing information about archive names.<br>This file is not required, but can be useful for Death Stranding as its archives have unreadable names.</html>");

            panel.add(label, "gap ind");
            panel.add(archiveInfoFilePath, "wrap");

            UIUtils.addOpenFileAction(archiveInfoFilePath, "Select archive information file", filter);
            UIUtils.installInputValidator(archiveInfoFilePath, new ExistingFileValidator(archiveInfoFilePath, filter, false), this);
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
        archiveInfoFilePath.setText(container.getPackfileMetadataPath() == null ? null : container.getPackfileMetadataPath().toString());
        fileListingsPath.setText(container.getFileListingsPath() == null ? null : container.getFileListingsPath().toString());
    }

    public void save(@NotNull ProjectContainer container) {
        container.setName(projectName.getText());
        container.setType((GameType) Objects.requireNonNull(projectType.getSelectedItem()));
        container.setExecutablePath(Path.of(executableFilePath.getText()));
        container.setPackfilesPath(Path.of(archiveFolderPath.getText()));
        container.setCompressorPath(Path.of(compressorPath.getText()));
        container.setTypeMetadataPath(Path.of(rttiInfoFilePath.getText()));
        container.setPackfileMetadataPath(archiveInfoFilePath.getText().isEmpty() ? null : Path.of(archiveInfoFilePath.getText()));
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
}
