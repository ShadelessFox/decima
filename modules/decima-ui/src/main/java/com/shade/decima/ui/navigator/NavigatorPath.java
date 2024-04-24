package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.util.FilePath;
import com.shade.util.NotNull;

public record NavigatorPath(@NotNull String projectId, @NotNull String packfileId, @NotNull FilePath filePath) {
    @NotNull
    public static NavigatorPath of(@NotNull ProjectContainer container, @NotNull Archive archive, @NotNull FilePath filePath) {
        return new NavigatorPath(container.getId().toString(), archive.getId(), filePath);
    }
}
