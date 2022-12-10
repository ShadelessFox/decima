package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.util.NotNull;

public class DMFSceneMetaData {
    public String generator;
    public int version;

    public DMFSceneMetaData() {
        super();
    }

    public DMFSceneMetaData(@NotNull String generator, int version) {
        super();
        this.generator = generator;
        this.version = version;
    }


}
