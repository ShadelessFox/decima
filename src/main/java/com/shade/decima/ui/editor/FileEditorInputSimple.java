package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.file.Path;
import java.util.StringJoiner;

public record FileEditorInputSimple(@NotNull Path path, @NotNull Project project) implements FileEditorInput {
    @NotNull
    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Nullable
    @Override
    public String getDescription() {
        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Project: " + project.getContainer().getName());
        joiner.add("Path: " + path);
        return joiner.toString();
    }

    @NotNull
    @Override
    public Path getPath() {
        return path;
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public boolean representsSameResource(@NotNull EditorInput other) {
        return equals(other);
    }
}
