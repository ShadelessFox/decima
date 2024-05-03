package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.platform.model.SaveableElement;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.lazy.LazyEditorInput;
import com.shade.platform.ui.editors.lazy.UnloadableEditorInput;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;

public record FileEditorInputSimple(@NotNull Path path, @NotNull Project project) implements FileEditorInput, UnloadableEditorInput, SaveableElement {
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
        if (other instanceof FileEditorInputLazy o) {
            return o.container().equals(project.getContainer().getId())
                && o.path().equals(path);
        }
        return equals(other);
    }

    @NotNull
    @Override
    public LazyEditorInput unloadInput() {
        return new FileEditorInputLazy(project.getContainer().getId(), path, false);
    }

    @Override
    public void saveState(@NotNull Map<String, Object> state) {
        state.put("project", project.getContainer().getId().toString());
        state.put("path", path.toAbsolutePath().toString());
    }

    @NotNull
    @Override
    public String getFactoryId() {
        return FileEditorInputFactory.ID;
    }
}
