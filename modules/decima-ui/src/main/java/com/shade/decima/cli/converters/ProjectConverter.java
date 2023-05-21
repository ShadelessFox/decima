package com.shade.decima.cli.converters;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import picocli.CommandLine.ITypeConverter;

import java.util.UUID;

public class ProjectConverter implements ITypeConverter<Project> {
    @Override
    public Project convert(String value) throws Exception {
        final UUID id = UUID.fromString(value);
        final ProjectManager manager = ProjectManager.getInstance();

        for (ProjectContainer container : manager.getProjects()) {
            if (container.getId().equals(id)) {
                return manager.openProject(container);
            }
        }

        throw new IllegalArgumentException("Can't find project '" + value + "'");
    }
}
