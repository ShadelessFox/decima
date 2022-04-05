package com.shade.decima.ui.action.impl.editors;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.ApplicationFrame;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(id = "com.shade.decima.ui.actions.impl.editors.CloseAllAction", name = "Close &All", description = "Close all editors")
@ActionContribution(path = "popup:editor", position = 2)
public class CloseAllAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final ApplicationFrame frame = Application.getFrame();
        frame.getEditorsPane().removeAll();
    }
}
