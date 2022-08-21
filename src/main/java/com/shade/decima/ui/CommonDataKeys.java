package com.shade.decima.ui;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.platform.model.data.DataKey;

public interface CommonDataKeys {
    DataKey<Workspace> WORKSPACE_KEY = new DataKey<>("workspace", Workspace.class);

    DataKey<Project> PROJECT_KEY = new DataKey<>("project", Project.class);
    DataKey<ProjectContainer> PROJECT_CONTAINER_KEY = new DataKey<>("projectContainer", ProjectContainer.class);
}
