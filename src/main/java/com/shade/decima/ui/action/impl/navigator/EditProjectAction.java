package com.shade.decima.ui.action.impl.navigator;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.dialogs.BaseEditDialog;
import com.shade.decima.ui.dialogs.ProjectEditDialog;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "Edit Project\u2026", description = "Edit the focused project")
@ActionContribution(path = "popup:navigator", position = 1)
public class EditProjectAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ApplicationFrame frame = Application.getFrame();

        if (frame.getNavigator().getTree().getLastSelectedPathComponent() instanceof NavigatorProjectNode node) {
            if (!node.needsInitialization()) {
                JOptionPane.showMessageDialog(frame, "Loaded project can't be edited", "Edit project", JOptionPane.ERROR_MESSAGE);
                return;
            }

            final Workspace workspace = frame.getWorkspace();
            final ProjectContainer container = node.getContainer();
            final ProjectEditDialog dialog = new ProjectEditDialog(true);

            dialog.load(container);

            if (dialog.showDialog(Application.getFrame()) == BaseEditDialog.OK_ID) {
                dialog.save(container);
                workspace.updateProject(container, true, true);
            }
        }
    }
}
