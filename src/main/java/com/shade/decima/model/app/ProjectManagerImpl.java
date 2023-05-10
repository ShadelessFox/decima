package com.shade.decima.model.app;

import com.shade.platform.model.Service;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.prefs.Preferences;

@Service(ProjectManager.class)
@Persistent("ProjectManager")
public class ProjectManagerImpl implements ProjectManager, PersistableComponent<ProjectContainer[]> {
    private final Map<UUID, ProjectInfo> projects = new LinkedHashMap<>();
    private final List<ProjectChangeListener> listeners = new ArrayList<>();

    @Override
    public void addProject(@NotNull ProjectContainer container) {
        if (projects.containsKey(container.getId())) {
            throw new IllegalArgumentException("Project already exists: " + container.getId());
        }

        projects.putIfAbsent(container.getId(), new ProjectInfo(container));
        fireProjectEvent(ProjectChangeListener::projectAdded, container);
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
        fireProjectEvent(ProjectChangeListener::projectRemoved, container);
    }

    @Override
    public void updateProject(@NotNull ProjectContainer container) {
        final ProjectInfo info = projects.get(container.getId());

        if (info == null) {
            throw new IllegalArgumentException("Project does not exist: " + container.getId());
        }

        info.container = container;
        fireProjectEvent(ProjectChangeListener::projectUpdated, container);
    }

    @Nullable
    @Override
    public ProjectContainer getProject(@NotNull UUID id) {
        final ProjectInfo info = projects.get(id);

        if (info != null) {
            return info.container;
        } else {
            return null;
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
    public synchronized Project openProject(@NotNull ProjectContainer container) throws IOException {
        final ProjectInfo info = projects.get(container.getId());

        if (info == null) {
            throw new IllegalArgumentException("Project does not exist: " + container.getId());
        }

        if (info.project == null) {
            info.project = new Project(info.container);
            fireProjectEvent(ProjectChangeListener::projectOpened, container);
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
        fireProjectEvent(ProjectChangeListener::projectClosed, info.container);
    }

    @Override
    public void addProjectListener(@NotNull ProjectChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeProjectListener(@NotNull ProjectChangeListener listener) {
        listeners.remove(listener);
    }

    @Nullable
    @Override
    public ProjectContainer[] getState() {
        return getProjects();
    }

    @Override
    public void loadState(@NotNull ProjectContainer[] state) {
        assert projects.isEmpty();
        for (ProjectContainer container : state) {
            addProject(container);
        }
    }

    @Override
    public void noStateLoaded() {
        // Backward compatibility
        final Preferences root = Preferences.userRoot().node("decima-explorer").node("projects");

        for (String id : IOUtils.unchecked(root::childrenNames)) {
            addProject(new ProjectContainer(UUID.fromString(id), root.node(id)));
        }
    }

    private void fireProjectEvent(@NotNull BiConsumer<ProjectChangeListener, ProjectContainer> consumer, @NotNull ProjectContainer container) {
        for (ProjectChangeListener listener : listeners) {
            consumer.accept(listener, container);
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
