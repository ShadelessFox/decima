package com.shade.decima.model.app;

import com.shade.decima.model.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Workspace implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Workspace.class);

    private final Preferences preferences;
    private final List<Project> projects;
    private final List<ProjectChangeListener> listeners;

    public Workspace() {
        this.preferences = Preferences.userRoot().node("decima-explorer");
        this.projects = new ArrayList<>();
        this.listeners = new ArrayList<>();

        try {
            loadProjects();
        } catch (BackingStoreException e) {
            log.error("Error reading workspace projects", e);
        }
    }

    public void addProject(@NotNull Project project) {
        addProject(project, true);
    }

    public void addProject(@NotNull Project project, boolean reflect) {
        projects.add(project);

        if (reflect) {
            fireProjectChangeEvent(ProjectChangeListener::projectAdded, project);
        }
    }

    public void closeProject(@NotNull Project project, boolean reflect) {
        if (!projects.contains(project)) {
            return;
        }

        try {
            project.close();
        } catch (IOException e) {
            log.warn("Error closing project", e);
            return;
        }

        if (reflect) {
            fireProjectChangeEvent(ProjectChangeListener::projectClosed, project);
        }
    }

    public void removeProject(@NotNull Project project) {
        removeProject(project, true);
    }

    public void removeProject(@NotNull Project project, boolean reflect) {
        if (!projects.contains(project)) {
            return;
        }

        try {
            project.close();
        } catch (IOException e) {
            log.warn("Error closing project", e);
            return;
        }

        try {
            preferences.node("projects").node(project.getId()).removeNode();
        } catch (BackingStoreException e) {
            log.warn("Error deleting project", e);
            return;
        }

        if (reflect) {
            fireProjectChangeEvent(ProjectChangeListener::projectClosed, project);
            fireProjectChangeEvent(ProjectChangeListener::projectRemoved, project);
        }

        projects.remove(project);
    }

    public void addProjectChangeListener(@NotNull ProjectChangeListener listener) {
        listeners.add(listener);
    }

    private void fireProjectChangeEvent(@NotNull BiConsumer<ProjectChangeListener, Project> consumer, @NotNull Project project) {
        if (listeners.isEmpty()) {
            return;
        }
        for (ProjectChangeListener listener : listeners) {
            consumer.accept(listener, project);
        }
    }

    @NotNull
    public List<Project> getProjects() {
        return projects;
    }

    @NotNull
    public Preferences getPreferences() {
        return preferences;
    }

    @Override
    public void close() throws IOException {
        for (Project project : projects) {
            closeProject(project, false);
        }

        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            log.warn("Error flushing preferences", e);
        }
    }

    private void loadProjects() throws BackingStoreException {
        final Preferences root = preferences.node("projects");

        for (String id : root.childrenNames()) {
            addProject(new Project(id, root.node(id)), false);
        }
    }
}
