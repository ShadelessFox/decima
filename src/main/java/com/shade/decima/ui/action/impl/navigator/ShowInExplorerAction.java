package com.shade.decima.ui.action.impl.navigator;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "Show in navigator", description = "Show the focused element in the system explorer")
@ActionContribution(path = "popup:navigator", separator = ActionContribution.SEPARATOR_AFTER)
public class ShowInExplorerAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ApplicationFrame frame = Application.getFrame();
        final NavigatorTree tree = frame.getNavigator();
        final Object component = tree.getTree().getLastSelectedPathComponent();

        if (component instanceof NavigatorPackfileNode node) {
            UIUtils.browseFileDirectory(node.getPackfile().getPath());
        } else if (component instanceof NavigatorProjectNode node) {
            UIUtils.browseFileDirectory(node.getContainer().getExecutablePath());
        }
    }
}
