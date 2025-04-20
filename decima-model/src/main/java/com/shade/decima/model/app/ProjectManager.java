package com.shade.decima.model.app;

import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.messages.Topic;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public interface ProjectManager {
    Topic<ProjectChangeListener> PROJECTS = Topic.create("projects", ProjectChangeListener.class);

    @NotNull
    static ProjectManager getInstance() {
        return ApplicationManager.getApplication().getService(ProjectManager.class);
    }

    void addProject(@NotNull ProjectContainer container);

    void removeProject(@NotNull ProjectContainer container);

    void updateProject(@NotNull ProjectContainer container);

    @Nullable
    ProjectContainer getProject(@NotNull UUID id);

    @NotNull
    ProjectContainer[] getProjects();

    @NotNull
    Collection<Project> getOpenProjects();

    @NotNull
    Project openProject(@NotNull ProjectContainer container) throws IOException;

    void closeProject(@NotNull Project project);
}
