package com.shade.decima.model.packfile;

import com.shade.decima.model.app.Project;
import com.shade.util.NotNull;

import java.io.IOException;

public interface PackfileProvider {
    @NotNull
    PackfileInfo[] getPackfiles(@NotNull Project project) throws IOException;
}
