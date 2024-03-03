package com.shade.decima.model.app.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.PackfileInfo;
import com.shade.decima.model.packfile.PackfileProvider;
import com.shade.util.NotNull;

import java.io.IOException;

public class HFWPackfileProvider implements PackfileProvider {
    @NotNull
    @Override
    public PackfileInfo[] getPackfiles(@NotNull Project project) throws IOException {
        return new PackfileInfo[0];
    }
}
