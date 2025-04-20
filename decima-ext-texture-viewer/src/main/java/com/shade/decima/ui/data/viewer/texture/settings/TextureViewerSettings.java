package com.shade.decima.ui.data.viewer.texture.settings;

import com.shade.decima.ui.settings.SettingsChangeListener;
import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.messages.Topic;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@Service(TextureViewerSettings.class)
@Persistent("TextureViewerSettings")
public class TextureViewerSettings implements PersistableComponent<TextureViewerSettings> {
    public static final Topic<SettingsChangeListener> SETTINGS = Topic.create("texture viewer settings", SettingsChangeListener.class);

    public boolean showGrid = false;
    public int showGridWhenZoomEqualOrMoreThan = 3;
    public int showGridEveryNthPixel = 1;
    public boolean showOutline = true;

    @NotNull
    public static TextureViewerSettings getInstance() {
        return ApplicationManager.getApplication().getService(TextureViewerSettings.class);
    }

    @Nullable
    @Override
    public TextureViewerSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull TextureViewerSettings state) {
        showGrid = state.showGrid;
        showGridWhenZoomEqualOrMoreThan = state.showGridWhenZoomEqualOrMoreThan;
        showGridEveryNthPixel = state.showGridEveryNthPixel;
        showOutline = state.showOutline;
    }
}
