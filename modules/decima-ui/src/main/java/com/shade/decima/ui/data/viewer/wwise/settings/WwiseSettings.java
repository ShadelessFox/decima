package com.shade.decima.ui.data.viewer.wwise.settings;

import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@Service(WwiseSettings.class)
@Persistent("WwiseSettings")
public class WwiseSettings implements PersistableComponent<WwiseSettings> {
    public String ww2oggPath;
    public String ww2oggCodebooksPath;
    public String revorbPath;
    public String ffmpegPath;

    @NotNull
    public static WwiseSettings getInstance() {
        return ApplicationManager.getApplication().getService(WwiseSettings.class);
    }

    @Nullable
    @Override
    public WwiseSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull WwiseSettings state) {
        ww2oggPath = state.ww2oggPath;
        ww2oggCodebooksPath = state.ww2oggCodebooksPath;
        revorbPath = state.revorbPath;
        ffmpegPath = state.ffmpegPath;
    }
}
