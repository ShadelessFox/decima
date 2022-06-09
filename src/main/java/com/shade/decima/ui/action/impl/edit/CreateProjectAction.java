package com.shade.decima.ui.action.impl.edit;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.dialogs.BaseEditDialog;
import com.shade.decima.ui.dialogs.ProjectEditDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.UUID;
import java.util.prefs.Preferences;

@ActionRegistration(name = "New &Project\u2026", description = "Create a new project")
@ActionContribution(path = "menu:file", position = 1)
public class CreateProjectAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ProjectEditDialog dialog = new ProjectEditDialog(Application.getFrame(), false);

        dialog.load(null);

        if (dialog.open() == BaseEditDialog.OK_ID) {
            final Workspace workspace = Application.getFrame().getWorkspace();
            final String id = UUID.randomUUID().toString();
            final Preferences node = workspace.getPreferences().node("projects").node(id);

            dialog.save(node);
            workspace.addProject(new Project(id, node));
        }
    }
}
