package com.shade.decima.ui.action.impl.edit;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.base.GameType;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.dialogs.BaseEditDialog;
import com.shade.decima.ui.dialogs.ProjectEditDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.UUID;

@ActionRegistration(name = "New &Project\u2026", description = "Create a new project")
@ActionContribution(path = "menu:file", position = 1)
public class CreateProjectAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ProjectEditDialog dialog = new ProjectEditDialog(false);
        final ProjectContainer container = new ProjectContainer(UUID.randomUUID(), "New project", GameType.DS, Path.of(""), Path.of(""), Path.of(""), Path.of(""), Path.of(""));

        dialog.load(container);

        if (dialog.showDialog(Application.getFrame()) == BaseEditDialog.OK_ID) {
            final Workspace workspace = Application.getFrame().getWorkspace();
            dialog.save(container);
            workspace.addProject(container, true, true);
        }
    }
}
