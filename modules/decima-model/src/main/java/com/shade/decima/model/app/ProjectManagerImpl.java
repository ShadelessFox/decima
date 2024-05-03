package com.shade.decima.model.app;

import com.shade.platform.model.Service;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.prefs.Preferences;

@Service(ProjectManager.class)
@Persistent("ProjectManager")
public class ProjectManagerImpl implements ProjectManager, PersistableComponent<ProjectContainer[]> {
    private final Map<UUID, ProjectInfo> projects = new LinkedHashMap<>();

    @Override
    public void addProject(@NotNull ProjectContainer container) {
        if (projects.containsKey(container.getId())) {
            throw new IllegalArgumentException("Project already exists: " + container.getId());
        }

        projects.put(container.getId(), new ProjectInfo(container));
        MessageBus.getInstance().publisher(PROJECTS).projectAdded(container);
    }

    @Override
    public void removeProject(@NotNull ProjectContainer container) {
        final ProjectInfo info = projects.get(container.getId());

        if (info == null) {
            throw new IllegalArgumentException("Project does not exist: " + container.getId());
        }

        if (info.project != null) {
            closeProject(info.project);
        }

        projects.remove(container.getId());
        MessageBus.getInstance().publisher(PROJECTS).projectRemoved(container);
    }

    @Override
    public void updateProject(@NotNull ProjectContainer container) {
        final ProjectInfo info = projects.get(container.getId());

        if (info == null) {
            throw new IllegalArgumentException("Project does not exist: " + container.getId());
        }

        info.container = container;
        MessageBus.getInstance().publisher(PROJECTS).projectUpdated(container);
    }

    @NotNull
    @Override
    public ProjectContainer getProject(@NotNull UUID id) {
        final ProjectInfo info = projects.get(id);

        if (info != null) {
            return info.container;
        } else {
            throw new IllegalArgumentException("Can't find project '" + id + "'");
        }
    }

    @NotNull
    @Override
    public ProjectContainer[] getProjects() {
        return projects.values().stream()
            .map(info -> info.container)
            .toArray(ProjectContainer[]::new);
    }

    @NotNull
    @Override
    public Collection<Project> getOpenProjects() {
        return projects.values().stream()
            .map(info -> info.project)
            .filter(Objects::nonNull)
            .toList();
    }

    @NotNull
    @Override
    public synchronized Project openProject(@NotNull ProjectContainer container) throws IOException {
        final ProjectInfo info = projects.get(container.getId());

        if (info == null) {
            throw new IllegalArgumentException("Project does not exist: " + container.getId());
        }

        if (info.project == null) {
            info.project = new Project(info.container);
            MessageBus.getInstance().publisher(PROJECTS).projectOpened(container);
        }

        return info.project;
    }

    @Override
    public void closeProject(@NotNull Project project) {
        final ProjectInfo info = projects.get(project.getContainer().getId());

        if (info == null) {
            throw new IllegalArgumentException("Project does not exist: " + project.getContainer().getId());
        }

        try {
            project.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        info.project = null;
        MessageBus.getInstance().publisher(PROJECTS).projectClosed(info.container);
    }

    @Nullable
    @Override
    public ProjectContainer[] getState() {
        return getProjects();
    }

    @Override
    public void loadState(@NotNull ProjectContainer[] state) {
        for (ProjectContainer container : state) {
            projects.put(container.getId(), new ProjectInfo(container));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void noStateLoaded() {
        // Backward compatibility
        final Preferences root = Preferences.userRoot().node("decima-explorer").node("projects");

        for (String id : IOUtils.unchecked(root::childrenNames)) {
            final ProjectContainer container = new ProjectContainer(UUID.fromString(id), root.node(id));
            projects.put(container.getId(), new ProjectInfo(container));
        }
    }

    private static class ProjectInfo {
        private ProjectContainer container;
        private Project project;

        public ProjectInfo(@NotNull ProjectContainer container) {
            this.container = container;
        }
    }
}
