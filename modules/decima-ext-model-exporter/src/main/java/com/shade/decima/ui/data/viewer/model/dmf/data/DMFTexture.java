package com.shade.decima.ui.data.viewer.model.dmf.data;

import com.shade.util.NotNull;

public class DMFTexture {
    public final String name;
    public final int bufferId;
    public String usageType;

    public DMFTexture(@NotNull String name, int bufferId) {
        this.name = name;
        this.bufferId = bufferId;
    }

    @NotNull
    public static DMFTexture nonExportableTexture(@NotNull String name) {
        return new DMFTexture(name, -1);
    }
}
