package com.shade.decima.model.viewer.settings;

import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@Service(ModelViewerSettings.class)
@Persistent("ModelViewerSettings")
public class ModelViewerSettings implements PersistableComponent<ModelViewerSettings> {
    public int fieldOfView = 60;
    public float sensitivity = 1.0f;
    public float nearClip = 0.01f;
    public float farClip = 1000.0f;

    @NotNull
    public static ModelViewerSettings getInstance() {
        return ApplicationManager.getApplication().getService(ModelViewerSettings.class);
    }

    @Nullable
    @Override
    public ModelViewerSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ModelViewerSettings state) {
        fieldOfView = state.fieldOfView;
        sensitivity = state.sensitivity;
        nearClip = state.nearClip;
        farClip = state.farClip;
    }
}
