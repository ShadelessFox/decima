package com.shade.decima.model.packfile;

import com.shade.decima.model.rtti.objects.Language;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public class PackfileInfo {
    private final String name;
    private final Language lang;

    public PackfileInfo(@NotNull String name, @Nullable Language lang) {
        this.name = name;
        this.lang = lang;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public Language getLang() {
        return lang;
    }
}
