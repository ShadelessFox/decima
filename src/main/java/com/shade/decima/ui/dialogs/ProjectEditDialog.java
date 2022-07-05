package com.shade.decima.ui.dialogs;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.validators.ExistingFileValidator;
import com.shade.decima.ui.controls.validators.NotEmptyValidator;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.Objects;
import java.util.prefs.Preferences;

public class ProjectEditDialog extends BaseEditDialog {
    private final boolean edit;

    private final JTextField projectUuid;
    private final JTextField projectName;
    private final JComboBox<GameType> projectType;
    private final JTextField executableFilePath;
    private final JTextField archiveFolderPath;
    private final JTextField compressorPathText;
    private final JTextField rttiInfoFilePath;
    private final JTextField archiveInfoFilePath;

    public ProjectEditDialog(boolean edit) {
        this.edit = edit;

        this.projectUuid = new JTextField();
        this.projectUuid.setEditable(false);
        this.projectName = new JTextField("New project");
        this.projectType = new JComboBox<>(GameType.values());
        this.executableFilePath = new JTextField();
        this.archiveFolderPath = new JTextField();
        this.compressorPathText = new JTextField();
        this.rttiInfoFilePath = new JTextField();
        this.archiveInfoFilePath = new JTextField();
    }

    public int showDialog(@Nullable JFrame owner) {
        return super.showDialog(owner, edit ? "Edit Project" : "New Project");
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("insets dialog", "[fill][grow,fill,250lp]", ""));

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

            panel.add(new JLabel("Game executable path:"));
            panel.add(executableFilePath, "wrap");

            UIUtils.addOpenFileAction(executableFilePath, "Select game executable", filter);
            UIUtils.installInputValidator(executableFilePath, new ExistingFileValidator(executableFilePath, filter), this);
        }

        {
            panel.add(new JLabel("Game packfile folder path:"));
            panel.add(archiveFolderPath, "wrap");

            UIUtils.addOpenDirectoryAction(archiveFolderPath, "Select folder containing game archives");
            UIUtils.installInputValidator(archiveFolderPath, new ExistingFileValidator(archiveFolderPath, null), this);
        }

        {
            final FileExtensionFilter filter = new FileExtensionFilter("Oodle Library File", "dll");

            panel.add(new JLabel("Oodle library path:"));
            panel.add(compressorPathText, "wrap");

            UIUtils.addOpenFileAction(compressorPathText, "Select Oodle library", filter);
            UIUtils.installInputValidator(compressorPathText, new ExistingFileValidator(compressorPathText, filter), this);
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
            UIUtils.installInputValidator(archiveInfoFilePath, new ExistingFileValidator(archiveInfoFilePath, filter), this);
        }

        return panel;
    }

    @Nullable
    @Override
    protected JComponent getDefaultComponent() {
        return projectName;
    }

    @Override
    public void load(@Nullable Preferences preferences) {
        if (preferences == null) {
            return;
        }

        projectUuid.setText(preferences.name());
        projectName.setText(preferences.get("game_name", null));
        projectType.setSelectedItem(GameType.valueOf(preferences.get("game_type", null)));
        executableFilePath.setText(preferences.get("game_executable_path", null));
        archiveFolderPath.setText(preferences.get("game_archive_root_path", null));
        compressorPathText.setText(preferences.get("game_compressor_path", null));
        rttiInfoFilePath.setText(preferences.get("game_rtti_meta_path", null));
        archiveInfoFilePath.setText(preferences.get("game_archive_meta_path", null));
    }

    @Override
    public void save(@Nullable Preferences preferences) {
        if (preferences == null) {
            return;
        }

        preferences.put("game_name", projectName.getText());
        preferences.put("game_type", ((GameType) Objects.requireNonNull(projectType.getSelectedItem())).name());
        preferences.put("game_executable_path", executableFilePath.getText());
        preferences.put("game_archive_root_path", archiveFolderPath.getText());
        preferences.put("game_compressor_path", compressorPathText.getText());
        preferences.put("game_rtti_meta_path", rttiInfoFilePath.getText());

        if (!archiveInfoFilePath.getText().isEmpty()) {
            preferences.put("game_archive_meta_path", archiveInfoFilePath.getText());
        }
    }

    @Override
    public boolean isComplete() {
        return UIUtils.isValid(projectName) && UIUtils.isValid(executableFilePath) && UIUtils.isValid(archiveFolderPath) && UIUtils.isValid(rttiInfoFilePath) && UIUtils.isValid(compressorPathText);
    }
}
