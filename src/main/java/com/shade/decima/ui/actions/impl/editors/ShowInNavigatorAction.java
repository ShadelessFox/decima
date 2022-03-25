package com.shade.decima.ui.actions.impl.editors;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.actions.ActionContribution;
import com.shade.decima.ui.actions.ActionRegistration;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

@ActionRegistration(id = "com.shade.decima.ui.actions.impl.editors.ShowInNavigatorAction", name = "&Show in Navigator", description = "Selects the focused file in the navigator")
@ActionContribution(path = "popup:editor", separator = ActionContribution.SEPARATOR_BEFORE)
public class ShowInNavigatorAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ApplicationFrame frame = Application.getFrame();
        final JTree navigator = frame.getNavigator().getTree();
        final TreePath path = UIUtils.getPath(frame.getFocusedEditor().getNode());
        navigator.setSelectionPath(path);
        navigator.scrollPathToVisible(path);
        navigator.requestFocusInWindow();
    }
}
