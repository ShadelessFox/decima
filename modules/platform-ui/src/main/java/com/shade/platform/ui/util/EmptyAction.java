package com.shade.platform.ui.util;

import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

public final class EmptyAction extends AbstractAction {
    public EmptyAction(@NotNull String name) {
        super(name);
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // do nothing
    }
}
