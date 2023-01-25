package com.shade.decima.ui.data.viewer.model.model.dmf;

import com.shade.util.NotNull;

public class DMFExternalTexture extends DMFTexture {
    public String bufferFileName;

    public DMFExternalTexture(@NotNull String name) {
        super(name);
    }
}
