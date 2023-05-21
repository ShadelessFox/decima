package com.shade.decima.ui.editor.core.settings;

import com.shade.decima.ui.settings.SettingsChangeListener;
import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.messages.Topic;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@Service(CoreEditorSettings.class)
@Persistent("CoreEditorSettings")
public class CoreEditorSettings implements PersistableComponent<CoreEditorSettings> {
    public static final Topic<SettingsChangeListener> SETTINGS = Topic.create("core editor settings", SettingsChangeListener.class);

    public boolean showBreadcrumbs = true;
    public boolean showValuePanel = true;
    public boolean selectFirstEntry = true;

    @NotNull
    public static CoreEditorSettings getInstance() {
        return ApplicationManager.getApplication().getService(CoreEditorSettings.class);
    }

    @Nullable
    @Override
    public CoreEditorSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CoreEditorSettings state) {
        showBreadcrumbs = state.showBreadcrumbs;
        showValuePanel = state.showValuePanel;
        selectFirstEntry = state.selectFirstEntry;
    }
}
