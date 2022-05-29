package com.shade.decima.ui.action.impl.navigator;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "Delete Project\u2026", description = "Delete the focused project from the workspace")
@ActionContribution(path = "popup:navigator")
public class DeleteProjectAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ApplicationFrame frame = Application.getFrame();

        if (frame.getNavigator().getTree().getLastSelectedPathComponent() instanceof NavigatorProjectNode node) {
            final Project project = node.getProject();
            final int result = JOptionPane.showConfirmDialog(frame, "Do you really want to delete project '%s'?".formatted(project.getName()), "Delete project", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                frame.getWorkspace().removeProject(project);
            }
        }
    }
}
