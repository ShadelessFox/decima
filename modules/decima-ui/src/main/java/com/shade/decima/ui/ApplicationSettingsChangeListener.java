package com.shade.decima.ui;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface ApplicationSettingsChangeListener {
    default void fontChanged(@Nullable String fontFamily, int fontSize) {
        // do nothing by default
    }

    default void themeChanged(@NotNull String themeClassName) {
        // do nothing by default
    }
}
