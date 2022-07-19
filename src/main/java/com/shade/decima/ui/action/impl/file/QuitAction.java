package com.shade.decima.ui.action.impl.file;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "&Quit", accelerator = "ctrl Q")
@ActionContribution(path = "menu:file", position = 1000, separator = ActionContribution.SEPARATOR_BEFORE)
public class QuitAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        Application.getFrame().dispose();
    }
}
