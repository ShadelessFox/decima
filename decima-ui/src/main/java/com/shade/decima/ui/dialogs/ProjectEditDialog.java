package com.shade.decima.ui.dialogs;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.packfile.oodle.Oodle;
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
    private final boolean persisted;
    private final boolean editable;

    private final JTextField projectId;
    private final JTextField projectName;
    private final JComboBox<GameType> projectType;
    private final JTextField executableFilePath;
    private final JTextField archiveFolderPath;
    private final JTextField compressorPath;
    private final ColoredComponent compressorNote;

    public ProjectEditDialog(boolean persisted, boolean editable) {
        super(persisted ? "Edit Project" : "New Project");
        this.persisted = persisted;
        this.editable = editable;

        this.projectId = new JTextField();
        this.projectId.setEditable(false);

        this.projectName = new JTextField();
        this.projectName.setEditable(editable);

        this.projectType = new JComboBox<>(GameType.values());
        this.projectType.setEnabled(editable);

        this.executableFilePath = new JTextField();
        this.executableFilePath.setEditable(editable);
        this.executableFilePath.getDocument().addDocumentListener((DocumentAdapter) e -> {
            if (UIUtils.isValid(executableFilePath)) {
                fillValuesBasedOnGameExecutable(Path.of(executableFilePath.getText()));
            }
        });

        this.archiveFolderPath = new JTextField();
        this.archiveFolderPath.setEditable(editable);

        this.compressorPath = new JTextField();
        this.compressorPath.setEditable(editable);

        this.compressorNote = new ColoredComponent();
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

        if (!persisted) {
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

        if (persisted) {
            panel.add(new JLabel("Id:"), "gap ind");
            panel.add(projectId, "wrap");

            UIUtils.addCopyAction(projectId);
        }

        {
            panel.add(new JLabel("Name:"), "gap ind");
            panel.add(projectName, "wrap");

            UIUtils.installInputValidator(projectName, new NotEmptyValidator(projectName), this);
        }

        {
            panel.add(new JLabel("Game:"), "gap ind");
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
            final FileExtensionFilter filter = FileExtensionFilter.ofNativeLibrary("Oodle Library");

            final JLabel label = new JLabel("Oodle library:");
            label.setToolTipText("<html>Path to the Oodle library used for compressing/decompressing game data.<br>For most games, it's a file in the game's root folder called <kbd>oo2core_XXX" + "</kbd>.</html>");

            panel.add(label, "gap ind");
            panel.add(compressorPath, "wrap");
            panel.add(compressorNote, "hidemode 2,skip,wrap");

            UIUtils.addOpenFileAction(compressorPath, "Select Oodle library", filter);
            UIUtils.installInputValidator(compressorPath, new ExistingFileValidator(compressorPath, filter), this);
        }

        return panel;
    }

    @Nullable
    @Override
    protected JComponent createLeftButtonsPane() {
        if (editable) {
            return super.createLeftButtonsPane();
        }
        return UIUtils.createInfoLabel("To edit this project's configuration, close it first");
    }

    @Nullable
    @Override
    protected JComponent getDefaultComponent() {
        return projectName;
    }

    public void load(@NotNull ProjectContainer container) {
        projectId.setText(container.getId().toString());
        projectName.setText(container.getName());
        projectType.setSelectedItem(container.getType());
        executableFilePath.setText(container.getExecutablePath().toString());
        archiveFolderPath.setText(container.getPackfilesPath().toString());
        compressorPath.setText(container.getCompressorPath().toString());
    }

    public void save(@NotNull ProjectContainer container) {
        container.setName(projectName.getText());
        container.setType((GameType) Objects.requireNonNull(projectType.getSelectedItem()));
        container.setExecutablePath(Path.of(executableFilePath.getText()));
        container.setPackfilesPath(Path.of(archiveFolderPath.getText()));
        container.setCompressorPath(Path.of(compressorPath.getText()));
    }

    @Override
    public boolean isComplete() {
        return UIUtils.isValid(projectName)
            && UIUtils.isValid(executableFilePath)
            && UIUtils.isValid(archiveFolderPath)
            && UIUtils.isValid(compressorPath);
    }

    private void fillValuesBasedOnGameExecutable(@NotNull Path path) {
        final String newFilename = IOUtils.getBasename(path).toLowerCase(Locale.ROOT);

        switch (newFilename) {
            case "ds" -> {
                setIfEmptyOrOldValue(archiveFolderPath, Path.of(archiveFolderPath.getText()), path.resolveSibling("data"));
                setIfEmptyOrOldValue(compressorPath, Path.of(compressorPath.getText()), path.resolveSibling("oo2core_7_win64.dll"));
            }
            case "horizonzerodawn" -> {
                setIfEmptyOrOldValue(archiveFolderPath, Path.of(archiveFolderPath.getText()), path.resolveSibling("Packed_DX12"));
                setIfEmptyOrOldValue(compressorPath, Path.of(compressorPath.getText()), path.resolveSibling("oo2core_3_win64.dll"));
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
}
