package com.shade.platform.ui;

import com.shade.util.NotNull;

import java.util.prefs.Preferences;

public interface SaveableElement {
    void saveState(@NotNull Preferences pref);

    @NotNull
    String getFactoryId();
}
