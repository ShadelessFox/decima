package com.shade.platform.ui.views;

import com.shade.util.NotNull;

import javax.swing.*;

public interface View {
    @NotNull
    JComponent createComponent();

    void setFocus();
}
