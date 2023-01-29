package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

public class DMFTexture {
    public String name;
    public DMFDataType dataType;
    public int bufferSize;
    public String usageType;

    public DMFTexture(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public static DMFTexture nonExportableTexture(@NotNull String name) {
        DMFTexture texture = new DMFTexture(name);
        texture.dataType = DMFDataType.UNSUPPORTED;
        texture.bufferSize = -1;
        return texture;
    }
}
