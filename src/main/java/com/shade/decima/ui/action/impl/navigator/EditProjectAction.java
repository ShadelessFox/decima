package com.shade.decima.ui.action.impl.navigator;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.dialogs.BaseEditDialog;
import com.shade.decima.ui.dialogs.ProjectEditDialog;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

@ActionRegistration(name = "Edit Project\u2026", description = "Edit the focused project")
@ActionContribution(path = "popup:navigator")
public class EditProjectAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ApplicationFrame frame = Application.getFrame();

        if (frame.getNavigator().getTree().getLastSelectedPathComponent() instanceof NavigatorProjectNode node) {
            if (!node.needsInitialization()) {
                JOptionPane.showMessageDialog(frame, "Loaded project can't be edited", "Edit project", JOptionPane.ERROR_MESSAGE);
                return;
            }

            final Project project = node.getProject();
            final Preferences pref = frame.getWorkspace().getPreferences().node("projects").node(project.getId());
            final ProjectEditDialog dialog = new ProjectEditDialog(frame, true);

            dialog.load(pref);

            if (dialog.open() == BaseEditDialog.OK_ID) {
                dialog.save(pref);
            }
        }
    }
}
