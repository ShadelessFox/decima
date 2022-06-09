package com.shade.decima.ui.dialogs;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.controls.validators.ExistingFileValidator;
import com.shade.decima.ui.controls.validators.NotEmptyValidator;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Objects;
import java.util.prefs.Preferences;

public class ProjectEditDialog extends BaseEditDialog {
    private JTextField projectName;
    private JComboBox<GameType> projectType;
    private JTextField executableFilePath;
    private JTextField archiveFolderPath;
    private JTextField compressorPathText;
    private JTextField rttiInfoFilePath;
    private JTextField archiveInfoFilePath;

    public ProjectEditDialog(@Nullable JFrame owner, boolean edit) {
        super(owner, edit ? "Edit Project" : "New Project");
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("insets dialog", "[fill][grow,fill,250lp]", ""));

        this.projectName = new JTextField("New project");
        this.projectType = new JComboBox<>(GameType.values());
        this.executableFilePath = new JTextField();
        this.archiveFolderPath = new JTextField();
        this.compressorPathText = new JTextField();
        this.rttiInfoFilePath = new JTextField();
        this.archiveInfoFilePath = new JTextField();

        {
            panel.add(new JLabel("Project name:"), "cell 0 0");
            panel.add(projectName, "cell 1 0");

            UIUtils.installInputValidator(projectName, new NotEmptyValidator(projectName), this);
        }

        {
            panel.add(new JLabel("Project type:"), "cell 0 1");
            panel.add(projectType, "cell 1 1");
        }

        {
            final FileNameExtensionFilter filter = new FileNameExtensionFilter("Executable File (*.exe)", "exe");

            panel.add(new JLabel("Game executable path:"), "cell 0 2");
            panel.add(executableFilePath, "cell 1 2");

            UIUtils.addOpenFileAction(executableFilePath, "Select game executable", filter);
            UIUtils.installInputValidator(executableFilePath, new ExistingFileValidator(executableFilePath, filter), this);
        }

        {
            panel.add(new JLabel("Game packfile folder path:"), "cell 0 3");
            panel.add(archiveFolderPath, "cell 1 3");

            UIUtils.addOpenDirectoryAction(archiveFolderPath, "Select folder containing game archives");
            UIUtils.installInputValidator(archiveFolderPath, new ExistingFileValidator(archiveFolderPath, null), this);
        }

        {
            final FileNameExtensionFilter filter = new FileNameExtensionFilter("Oodle Library File (*.dll)", "dll");

            panel.add(new JLabel("Oodle library path:"), "cell 0 4");
            panel.add(compressorPathText, "cell 1 4");

            UIUtils.addOpenFileAction(compressorPathText, "Select Oodle library", filter);
            UIUtils.installInputValidator(compressorPathText, new ExistingFileValidator(compressorPathText, filter), this);
        }


        panel.add(new JSeparator(), "cell 0 5,span");

        {
            final FileNameExtensionFilter filter = new FileNameExtensionFilter("RTTI information (*.json,*.json.gz)", "json", "json.gz");

            panel.add(new JLabel("RTTI metadata path:"), "cell 0 6");
            panel.add(rttiInfoFilePath, "cell 1 6");

            UIUtils.addOpenFileAction(rttiInfoFilePath, "Select RTTI information file", filter);
            UIUtils.installInputValidator(rttiInfoFilePath, new ExistingFileValidator(rttiInfoFilePath, filter), this);
        }

        {
            final FileNameExtensionFilter filter = new FileNameExtensionFilter("Archive information (*.json,*.json.gz)", "json", "json.gz");

            final JLabel label = new JLabel("Packfile metadata path:");
            panel.add(label, "cell 0 7");
            panel.add(archiveInfoFilePath, "cell 1 7");

            UIUtils.addOpenFileAction(archiveInfoFilePath, "Select archive information file", filter);
            UIUtils.installInputValidator(archiveInfoFilePath, new ExistingFileValidator(archiveInfoFilePath, filter), this);
        }

        return panel;
    }

    @Override
    public void load(@Nullable Preferences preferences) {
        if (preferences == null) {
            return;
        }

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
