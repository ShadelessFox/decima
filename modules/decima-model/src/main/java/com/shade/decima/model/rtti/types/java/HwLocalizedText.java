package com.shade.decima.model.rtti.types.java;

import com.shade.util.NotNull;

public interface HwLocalizedText extends HwType {
    enum DisplayMode {
        /**
         * Shows the text if subtitles are enabled.
         */
        SHOW_IF_SUBTITLES_ENABLED,
        /**
         * Forces the text to be shown even if subtitles are disabled.
         */
        SHOW_ALWAYS,
        /**
         * Forces the text to never be shown even if subtitles are enabled.
         */
        SHOW_NEVER
    }

    int getLocalizationCount();

    @NotNull
    String getLanguage(int index);

    @NotNull
    String getTranslation(int index);

    void setTranslation(int index, @NotNull String translation);

    @NotNull
    DisplayMode getDisplayMode(int index);

    void setDisplayMode(int index, @NotNull DisplayMode mode);
}
