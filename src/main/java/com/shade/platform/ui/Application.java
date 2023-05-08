package com.shade.platform.ui;

import com.shade.util.NotNull;

import javax.swing.*;

public interface Application {
    @NotNull
    JFrame getFrame();

    <T> T getService(@NotNull Class<T> cls);
}
