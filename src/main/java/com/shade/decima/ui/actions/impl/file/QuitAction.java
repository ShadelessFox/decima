package com.shade.decima.ui.actions.impl.file;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.actions.ActionContribution;
import com.shade.decima.ui.actions.ActionRegistration;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(id = "com.shade.decima.ui.actions.impl.file.QuitAction", name = "&Quit", accelerator = "ctrl Q")
@ActionContribution(path = "menu:file", separator = ActionContribution.SEPARATOR_BEFORE)
public class QuitAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        Application.getFrame().dispose();
    }
}
