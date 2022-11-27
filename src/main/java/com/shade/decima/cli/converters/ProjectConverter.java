package com.shade.decima.cli.converters;

import com.shade.decima.cli.ApplicationCLI;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import picocli.CommandLine.ITypeConverter;

import java.util.List;
import java.util.UUID;

public class ProjectConverter implements ITypeConverter<Project> {
    @Override
    public Project convert(String value) throws Exception {
        final UUID id = UUID.fromString(value);
        final List<ProjectContainer> projects = ApplicationCLI.getWorkspace().getProjects();

        for (ProjectContainer container : projects) {
            if (container.getId().equals(id)) {
                final Project project = new Project(container);
                project.mountDefaults();

                return project;
            }
        }

        throw new IllegalArgumentException("Can't find project '" + value + "'");
    }
}
