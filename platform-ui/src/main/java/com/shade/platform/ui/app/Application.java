package com.shade.platform.ui.app;

import com.shade.util.NotNull;

import javax.swing.*;

public interface Application {
    void start(@NotNull String[] args);

    @NotNull
    JFrame getFrame();

    <T> T getService(@NotNull Class<T> cls);
}
