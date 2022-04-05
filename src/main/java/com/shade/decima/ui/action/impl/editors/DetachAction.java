package com.shade.decima.ui.action.impl.editors;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.EditorsPane;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.editor.EditorPane;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(id = "com.shade.decima.ui.action.impl.editors.DetachAction", name = "Detach", description = "Detaches currently focused editor to a separate window")
@ActionContribution(path = "popup:editor", separator = ActionContribution.SEPARATOR_BEFORE)
public class DetachAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ApplicationFrame frame = Application.getFrame();
        final EditorPane editor = frame.getEditorsPane().getFocusedEditor();

        if (editor == null) {
            return;
        }

        frame.getEditorsPane().closeEditor(editor.getNode());

        final JFrame child = new JFrame();

        final EditorsPane editors = new EditorsPane();
        editors.showEditor(editor.getNode());
        editors.addChangeListener(event -> {
            if (editors.getTabCount() == 0) {
                child.dispose();
            }
        });

        child.add(editors);
        child.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        child.setLocationRelativeTo(frame);
        child.pack();
        child.setVisible(true);
    }
}
