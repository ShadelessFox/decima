package com.shade.decima.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.messages.Topic;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@Service(ApplicationSettings.class)
@Persistent("ApplicationSettings")
public class ApplicationSettings implements PersistableComponent<ApplicationSettings> {
    public static final Topic<ApplicationSettingsChangeListener> SETTINGS = Topic.create("application settings", ApplicationSettingsChangeListener.class);

    public String themeClassName = FlatLightLaf.class.getName();
    public String customFontFamily;
    public int customFontSize;

    @NotNull
    public static ApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(ApplicationSettings.class);
    }

    @Nullable
    @Override
    public ApplicationSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ApplicationSettings state) {
        themeClassName = state.themeClassName;
        customFontFamily = state.customFontFamily;
        customFontSize = state.customFontSize;
    }
}
