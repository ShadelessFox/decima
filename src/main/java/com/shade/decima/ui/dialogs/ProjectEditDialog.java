package com.shade.decima.ui.dialogs;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.GameType;
import com.shade.decima.ui.controls.FileExtensionFilter;
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
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("insets 0", "[fill][grow,fill,250lp]", ""));

        if (edit) {
            panel.add(new JLabel("Project UUID:"));
            panel.add(projectUuid, "wrap");
            panel.add(new JSeparator(), "wrap,span");
        }

        {
            panel.add(new JLabel("Project name:"));
            panel.add(projectName, "wrap");

            UIUtils.installInputValidator(projectName, new NotEmptyValidator(projectName), this);
        }

        {
            panel.add(new JLabel("Project type:"));
            panel.add(projectType, "wrap");
        }

        {
            final FileExtensionFilter filter = new FileExtensionFilter("Executable File", "exe");

            final JLabel gameExecutablePathLabel = new JLabel("Game executable path:");
            gameExecutablePathLabel.setToolTipText("Path to a game's executable. Likely to be the only .exe file in the game's directory.");

            panel.add(gameExecutablePathLabel);
            panel.add(executableFilePath, "wrap");

            UIUtils.addOpenFileAction(executableFilePath, "Select game executable", filter);
            UIUtils.installInputValidator(executableFilePath, new ExistingFileValidator(executableFilePath, filter), this);
        }

        {
            final JLabel archiveFolderPathLabel = new JLabel("Game packfile folder path:");
            archiveFolderPathLabel.setToolTipText("Path to a folder with game archives. In most cases it has a bunch of .bin files in it.");

            panel.add(archiveFolderPathLabel);
            panel.add(archiveFolderPath, "wrap");

            UIUtils.addOpenDirectoryAction(archiveFolderPath, "Select folder containing game archives");
            UIUtils.installInputValidator(archiveFolderPath, new ExistingFileValidator(archiveFolderPath, null), this);
        }

        {
            final FileExtensionFilter filter = new FileExtensionFilter("Oodle Library File", "dll");

            final JLabel compressorPathLabel = new JLabel("Oodle library path:");
            compressorPathLabel.setToolTipText("A library required for working with data from game archives.\nIt's a .dll file located in the game's folder and has name that starts with oo2core.");

            panel.add(compressorPathLabel);
            panel.add(compressorPath, "wrap");

            UIUtils.addOpenFileAction(compressorPath, "Select Oodle library", filter);
            UIUtils.installInputValidator(compressorPath, new ExistingFileValidator(compressorPath, filter), this);
        }


        panel.add(new JSeparator(), "wrap,span");

        {
            final FileExtensionFilter filter = new FileExtensionFilter("RTTI information", "json", "json.gz");

            panel.add(new JLabel("RTTI metadata path:"));
            panel.add(rttiInfoFilePath, "wrap");

            UIUtils.addOpenFileAction(rttiInfoFilePath, "Select RTTI information file", filter);
            UIUtils.installInputValidator(rttiInfoFilePath, new ExistingFileValidator(rttiInfoFilePath, filter), this);
        }

        {
            final FileExtensionFilter filter = new FileExtensionFilter("Archive information", "json", "json.gz");

            final JLabel label = new JLabel("Packfile metadata path:");
            panel.add(label);
            panel.add(archiveInfoFilePath, "wrap");

            UIUtils.addOpenFileAction(archiveInfoFilePath, "Select archive information file", filter);
            UIUtils.installInputValidator(archiveInfoFilePath, new ExistingFileValidator(archiveInfoFilePath, filter, false), this);
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
    }

    public void save(@NotNull ProjectContainer container) {
        container.setName(projectName.getText());
        container.setType((GameType) Objects.requireNonNull(projectType.getSelectedItem()));
        container.setExecutablePath(Path.of(executableFilePath.getText()));
        container.setPackfilesPath(Path.of(archiveFolderPath.getText()));
        container.setCompressorPath(Path.of(compressorPath.getText()));
        container.setTypeMetadataPath(Path.of(rttiInfoFilePath.getText()));
        container.setPackfileMetadataPath(archiveInfoFilePath.getText().isEmpty() ? null : Path.of(archiveInfoFilePath.getText()));
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
