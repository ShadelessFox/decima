package com.shade.decima.ui.icon;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.shade.decima.model.util.NotNull;

import javax.swing.*;

public class Icons {
    public static final Icon NODE_BINARY = load("icons/nodes/binary.svg");

    private Icons() {
    }

    @NotNull
    private static Icon load(@NotNull String name) {
        return new FlatSVGIcon(name);
    }
}
