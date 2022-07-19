package com.shade.decima.ui.action.impl.navigator;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "Close Project", description = "Close the focused project")
@ActionContribution(path = "popup:navigator", position = 101)
public class CloseProjectAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent event) {
        final ApplicationFrame frame = Application.getFrame();
        final NavigatorTree tree = frame.getNavigator();

        if (tree.getTree().getLastSelectedPathComponent() instanceof NavigatorProjectNode node) {
            if (node.needsInitialization()) {
                return;
            }

            frame.getWorkspace().closeProject(node.getContainer(), true);
        }
    }
}
