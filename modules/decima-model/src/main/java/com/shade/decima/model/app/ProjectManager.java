package com.shade.decima.model.app;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.util.UUID;

public interface ProjectManager {
    void addProject(@NotNull ProjectContainer container);

    void removeProject(@NotNull ProjectContainer container);

    void updateProject(@NotNull ProjectContainer container);

    @Nullable
    ProjectContainer getProject(@NotNull UUID id);

    @NotNull
    ProjectContainer[] getProjects();

    @NotNull
    Project openProject(@NotNull ProjectContainer container) throws IOException;

    void closeProject(@NotNull Project project);

    void addProjectListener(@NotNull ProjectChangeListener listener);

    void removeProjectListener(@NotNull ProjectChangeListener listener);
}
