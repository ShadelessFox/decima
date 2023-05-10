package com.shade.decima.model.rtti.objects;

import com.shade.util.NotNull;

/**
 * Represents the RTTI type {@code ELanguage}
 */
public enum Language {
    UNKNOWN("Unknown"),
    ENGLISH("English"),
    FRENCH("French"),
    SPANISH("Spanish"),
    GERMAN("German"),
    ITALIAN("Italian"),
    DUTCH("Dutch"),
    PORTUGUESE("Portuguese"),
    CHINESE_TRADITIONAL("Chinese (Traditional)"),
    KOREAN("Korean"),
    RUSSIAN("Russian"),
    POLISH("Polish"),
    DANISH("Danish"),
    FINNISH("Finnish"),
    NORWEGIAN("Norwegian"),
    SWEDISH("Swedish"),
    JAPANESE("Japanese"),
    LATIN_SPANISH("Latin Spanish"),
    LATIN_PORTUGUESE("Latin Portuguese"),
    TURKISH("Turkish"),
    ARABIC("Arabic"),
    CHINESE_SIMPLIFIED("Chinese (Simplified)"),
    ENGLISH_UK("English (UK)"),
    GREEK("Greek"),
    CZECH("Czech"),
    HUNGARIAN("Hungarian");

    private final String label;

    Language(@NotNull String label) {
        this.label = label;
    }

    @NotNull
    public String getLabel() {
        return label;
    }
}
