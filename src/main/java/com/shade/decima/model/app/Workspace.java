package com.shade.decima.model.app;

import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Workspace implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Workspace.class);

    private final Preferences preferences;
    private final List<ProjectContainer> projects;
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

    public void addProject(@NotNull ProjectContainer container, boolean reflect, boolean persist) {
        projects.add(container);

        if (reflect) {
            fireProjectChangeEvent(ProjectChangeListener::projectAdded, container);
        }

        if (persist) {
            container.save(getProjectNode(container));
        }
    }

    public void updateProject(@NotNull ProjectContainer container, boolean reflect, boolean persist) {
        if (!projects.contains(container)) {
            return;
        }

        if (reflect) {
            fireProjectChangeEvent(ProjectChangeListener::projectUpdated, container);
        }

        if (persist) {
            container.save(getProjectNode(container));
        }
    }

    public void removeProject(@NotNull ProjectContainer container, boolean reflect) {
        if (!projects.contains(container)) {
            return;
        }

        try {
            getProjectNode(container).removeNode();
        } catch (BackingStoreException e) {
            log.warn("Error deleting project", e);
            return;
        }

        if (reflect) {
            fireProjectChangeEvent(ProjectChangeListener::projectClosed, container);
            fireProjectChangeEvent(ProjectChangeListener::projectRemoved, container);
        }

        projects.remove(container);
    }

    public void closeProject(@NotNull ProjectContainer container, boolean reflect) {
        if (!projects.contains(container)) {
            return;
        }

        if (reflect) {
            fireProjectChangeEvent(ProjectChangeListener::projectClosed, container);
        }
    }

    public void addProjectChangeListener(@NotNull ProjectChangeListener listener) {
        listeners.add(listener);
    }

    private void fireProjectChangeEvent(@NotNull BiConsumer<ProjectChangeListener, ProjectContainer> consumer, @NotNull ProjectContainer container) {
        if (listeners.isEmpty()) {
            return;
        }
        for (ProjectChangeListener listener : listeners) {
            consumer.accept(listener, container);
        }
    }

    @NotNull
    public List<ProjectContainer> getProjects() {
        return projects;
    }

    @NotNull
    public Preferences getPreferences() {
        return preferences;
    }

    @Override
    public void close() throws IOException {
        for (ProjectContainer container : projects) {
            closeProject(container, true);
        }

        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            log.warn("Error flushing preferences", e);
        }
    }

    @NotNull
    private Preferences getProjectNode(@NotNull ProjectContainer container) {
        return preferences.node("projects").node(container.getId().toString());
    }

    private void loadProjects() throws BackingStoreException {
        final Preferences root = preferences.node("projects");

        for (String id : root.childrenNames()) {
            addProject(new ProjectContainer(UUID.fromString(id), root.node(id)), false, false);
        }
    }
}
