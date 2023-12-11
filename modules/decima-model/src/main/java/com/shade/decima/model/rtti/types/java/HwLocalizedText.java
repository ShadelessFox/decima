package com.shade.decima.model.rtti.types.java;

import com.shade.util.NotNull;

public interface HwLocalizedText extends HwType {
    int getLocalizationCount();

    @NotNull
    String getLocalizationLanguage(int index);

    @NotNull
    String getLocalizationText(int index);
}
