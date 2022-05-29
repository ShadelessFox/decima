package com.shade.decima.ui.action.impl.editors;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.editor.PropertyEditorPane;
import com.shade.decima.ui.navigator.NavigatorTree;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "&Show in Navigator", description = "Selects the focused file in the navigator")
@ActionContribution(path = "popup:editor", separator = ActionContribution.SEPARATOR_BEFORE)
public class ShowInNavigatorAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ApplicationFrame frame = Application.getFrame();
        final PropertyEditorPane editor = frame.getEditorsPane().getFocusedEditor();

        if (editor != null) {
            final NavigatorTree navigator = frame.getNavigator();
            final JTree tree = navigator.getTree();
            final TreePath path = new TreePath(navigator.getModel().getPathToRoot(editor.getNode()));
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
            tree.requestFocusInWindow();
        }
    }
}
