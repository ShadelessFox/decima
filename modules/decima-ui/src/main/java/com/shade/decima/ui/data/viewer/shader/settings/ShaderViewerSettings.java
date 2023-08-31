package com.shade.decima.ui.data.viewer.shader.settings;

import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

@Service(ShaderViewerSettings.class)
@Persistent("ShaderViewerSettings")
public class ShaderViewerSettings implements PersistableComponent<ShaderViewerSettings> {
    public String d3dCompilerPath;
    public String dxCompilerPath;

    @NotNull
    public static ShaderViewerSettings getInstance() {
        return ApplicationManager.getApplication().getService(ShaderViewerSettings.class);
    }

    @Nullable
    @Override
    public ShaderViewerSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ShaderViewerSettings state) {
        d3dCompilerPath = state.d3dCompilerPath;
        dxCompilerPath = state.dxCompilerPath;
    }
}
