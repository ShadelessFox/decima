package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.SaveableElement;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.lazy.LazyEditorInput;
import com.shade.platform.ui.editors.lazy.UnloadableEditorInput;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.StringJoiner;
import java.util.prefs.Preferences;

public record NodeEditorInputSimple(@NotNull NavigatorFileNode node) implements NodeEditorInput, UnloadableEditorInput, SaveableElement {
    @NotNull
    @Override
    public String getName() {
        return node.getLabel();
    }

    @Nullable
    @Override
    public String getDescription() {
        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Project: " + node.getProject().getContainer().getName());
        joiner.add("Packfile: " + node.getPackfile().getName());
        joiner.add("Path: " + node.getPath().full());
        return joiner.toString();
    }

    @Override
    public boolean representsSameResource(@NotNull EditorInput other) {
        if (other instanceof NodeEditorInputLazy o) {
            return o.container().equals(node.getProjectContainer().getId())
                && o.packfile().equals(node.getPackfile().getPath().getFileName().toString())
                && o.path().equals(node.getPath());
        }
        return equals(other);
    }

    @NotNull
    @Override
    public NavigatorFileNode getNode() {
        return node;
    }

    @NotNull
    @Override
    public Project getProject() {
        return node.getProject();
    }

    @NotNull
    @Override
    public LazyEditorInput unloadInput() {
        return NodeEditorInputLazy.from(this).canLoadImmediately(false);
    }

    @Override
    public void saveState(@NotNull Preferences pref) {
        pref.put("project", node.getProject().getContainer().getId().toString());
        pref.put("packfile", node.getPackfile().getPath().getFileName().toString());
        pref.put("resource", node.getPath().full());
    }

    @NotNull
    @Override
    public String getFactoryId() {
        return NodeEditorInputFactory.ID;
    }
}
