package com.shade.decima.ui.action.impl.file;

import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "&Open\u2026", accelerator = "ctrl O")
@ActionContribution(path = "menu:file")
public class OpenAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("To be done eventually");
    }
}
