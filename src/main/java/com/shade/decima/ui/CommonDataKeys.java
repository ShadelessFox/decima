package com.shade.decima.ui;

import com.shade.decima.model.app.DataKey;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorManager;
import com.shade.decima.ui.editor.stack.EditorStack;

public interface CommonDataKeys {
    DataKey<Workspace> WORKSPACE_KEY = new DataKey<>("workspace", Workspace.class);

    DataKey<Project> PROJECT_KEY = new DataKey<>("project", Project.class);
    DataKey<ProjectContainer> PROJECT_CONTAINER_KEY = new DataKey<>("projectContainer", ProjectContainer.class);

    DataKey<Editor> EDITOR_KEY = new DataKey<>("editor", Editor.class);
    DataKey<EditorStack> EDITOR_STACK_KEY = new DataKey<>("editorStack", EditorStack.class);
    DataKey<EditorManager> EDITOR_MANAGER_KEY = new DataKey<>("editorManager", EditorManager.class);

    DataKey<Object> SELECTION_KEY = new DataKey<>("selection", Object.class);
}
