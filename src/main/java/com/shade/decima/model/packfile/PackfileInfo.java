package com.shade.decima.model.packfile;

import com.shade.decima.model.rtti.objects.Language;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public class PackfileInfo {
    private final String id;
    private final String name;
    private final Language lang;

    public PackfileInfo(@NotNull String id, @NotNull String name, @Nullable Language lang) {
        this.id = id;
        this.name = name;
        this.lang = lang;
    }

    @NotNull
    public String getId() {
        return id;
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
