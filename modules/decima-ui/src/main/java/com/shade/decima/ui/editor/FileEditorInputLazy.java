package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.platform.model.SaveableElement;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.lazy.LazyEditorInput;
import com.shade.platform.ui.editors.lazy.UnloadableEditorInput;
import com.shade.util.NotNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public record FileEditorInputLazy(@NotNull UUID container, @NotNull Path path, boolean canLoadImmediately) implements LazyEditorInput, UnloadableEditorInput, SaveableElement {
    public FileEditorInputLazy(@NotNull UUID container, @NotNull Path path) {
        this(container, path, true);
    }

    @NotNull
    @Override
    public EditorInput loadRealInput(@NotNull ProgressMonitor monitor) throws Exception {
        final ProjectManager manager = ProjectManager.getInstance();
        final ProjectContainer projectContainer = manager.getProject(container);
        final Project project = manager.openProject(projectContainer);
        return new FileEditorInputSimple(path, project);
    }

    @NotNull
    @Override
    public LazyEditorInput unloadInput() {
        return canLoadImmediately(false);
    }

    @NotNull
    @Override
    public LazyEditorInput canLoadImmediately(boolean value) {
        return new FileEditorInputLazy(container, path, value);
    }

    @NotNull
    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @NotNull
    @Override
    public String getDescription() {
        return path.toString();
    }

    @Override
    public boolean representsSameResource(@NotNull EditorInput other) {
        return false;
    }

    @Override
    public void saveState(@NotNull Map<String, Object> state) {
        state.put("project", container.toString());
        state.put("path", path.toAbsolutePath().toString());
    }

    @NotNull
    @Override
    public String getFactoryId() {
        return FileEditorInputFactory.ID;
    }
}
