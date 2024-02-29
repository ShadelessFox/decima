package com.shade.decima.ui.editor.impl;

import com.shade.decima.ui.menu.menus.EditMenu;
import com.shade.decima.ui.menu.menus.FileMenu;
import com.shade.decima.ui.menu.menus.ViewMenu;
import com.shade.platform.ui.editors.spi.EditorOnboarding;
import com.shade.platform.ui.editors.spi.EditorOnboardingProvider;
import com.shade.util.NotNull;

import java.util.List;

public class DefaultEditorOnboardingProvider implements EditorOnboardingProvider {
    @NotNull
    @Override
    public Iterable<EditorOnboarding> getOnboardings() {
        return List.of(
            new EditorOnboarding.Action(FileMenu.NewProjectItem.ID, "Create new project"),
            new EditorOnboarding.Action(EditMenu.FindFilesItem.ID, "Find files"),
            new EditorOnboarding.Action(ViewMenu.RecentFilesItem.ID, "Show recent files"),
            new EditorOnboarding.Text("Drop files here to open them")
        );
    }
}
