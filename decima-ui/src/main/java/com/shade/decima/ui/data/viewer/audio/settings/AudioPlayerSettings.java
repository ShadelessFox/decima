package com.shade.decima.ui.data.viewer.audio.settings;

import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@Service(AudioPlayerSettings.class)
@Persistent("WwiseSettings")
public class AudioPlayerSettings implements PersistableComponent<AudioPlayerSettings> {
    public String ww2oggPath;
    public String ww2oggCodebooksPath;
    public String revorbPath;
    public String ffmpegPath;

    @NotNull
    public static AudioPlayerSettings getInstance() {
        return ApplicationManager.getApplication().getService(AudioPlayerSettings.class);
    }

    @Nullable
    @Override
    public AudioPlayerSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AudioPlayerSettings state) {
        ww2oggPath = state.ww2oggPath;
        ww2oggCodebooksPath = state.ww2oggCodebooksPath;
        revorbPath = state.revorbPath;
        ffmpegPath = state.ffmpegPath;
    }
}
