package com.shade.decima.hfw.project;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.PackfileInfo;
import com.shade.decima.model.packfile.PackfileProvider;
import com.shade.util.NotNull;

public class HFWPackfileProvider implements PackfileProvider {
    @NotNull
    @Override
    public PackfileInfo[] getPackfiles(@NotNull Project project) {
        return new PackfileInfo[0];
    }
}
