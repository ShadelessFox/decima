package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.FilePath;
import com.shade.util.NotNull;

public record NavigatorPath(@NotNull String projectId, @NotNull String packfileId, @NotNull FilePath filePath) {
    @NotNull
    public static NavigatorPath of(@NotNull ProjectContainer container, @NotNull Packfile packfile, @NotNull FilePath filePath) {
        return new NavigatorPath(container.getId().toString(), packfile.getPath().getFileName().toString(), filePath);
    }
}
