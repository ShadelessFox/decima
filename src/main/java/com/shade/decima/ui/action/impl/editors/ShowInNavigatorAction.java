package com.shade.decima.ui.action.impl.editors;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.lazy.LazyEditorInput;
import com.shade.decima.ui.navigator.NavigatorTree;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "&Show in Navigator", description = "Selects the focused file in the navigator")
@ActionContribution(path = "popup:editor", position = 1000, separator = ActionContribution.SEPARATOR_BEFORE)
public class ShowInNavigatorAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ApplicationFrame frame = Application.getFrame();
        final Editor editor = frame.getEditorManager().getActiveEditor();

        if (editor != null && !(editor.getInput() instanceof LazyEditorInput)) {
            final NavigatorTree navigator = frame.getNavigator();
            final JTree tree = navigator.getTree();
            final TreePath path = new TreePath(navigator.getModel().getPathToRoot(editor.getInput().getNode()));
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
            tree.requestFocusInWindow();
        }
    }
}
