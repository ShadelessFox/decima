package com.shade.platform.ui.settings;

import com.shade.util.NotNull;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public interface SettingsPage {
    @NotNull
    JComponent createComponent(@NotNull PropertyChangeListener listener);

    void apply();

    void reset();

    boolean isModified();

    boolean isComplete();
}
