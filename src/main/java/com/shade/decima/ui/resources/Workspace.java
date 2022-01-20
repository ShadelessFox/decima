package com.shade.decima.ui.resources;

import com.shade.decima.util.NotNull;

import java.beans.PropertyChangeSupport;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class Workspace implements Closeable {
    public static final String PROJECTS_PROPERTY = "projectsProperty";

    private final PropertyChangeSupport propertyChangeSupport;
    private final Set<Project> projects;

    public Workspace() {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.projects = new LinkedHashSet<>();
    }

    public void addProject(@NotNull Project project) {
        projects.add(project);
        propertyChangeSupport.firePropertyChange(PROJECTS_PROPERTY, null, project);
    }

    public void removeProject(@NotNull Project project) {
        projects.remove(project);
        propertyChangeSupport.firePropertyChange(PROJECTS_PROPERTY, project, null);
    }

    @NotNull
    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    @NotNull
    public Collection<Project> getProjects() {
        return projects;
    }

    @Override
    public void close() throws IOException {
        for (Project project : projects) {
            project.close();
        }
    }
}
