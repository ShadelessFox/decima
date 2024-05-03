package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.ui.editor.ProjectEditorInput;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;

public record CoreEditorInput(@NotNull RTTICoreFile coreFile, @NotNull String name, @NotNull Project project) implements ProjectEditorInput {
    @NotNull
    @Override
    public Project getProject() {
        return project;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Project: " + project.getContainer().getName();
    }

    @Override
    public boolean representsSameResource(@NotNull EditorInput other) {
        return false;
    }
}
