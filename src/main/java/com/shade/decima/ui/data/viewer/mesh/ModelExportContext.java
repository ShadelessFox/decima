package com.shade.decima.ui.data.viewer.mesh;

import com.shade.decima.ui.data.viewer.mesh.dmf.DMFSceneFile;
import com.shade.util.NotNull;

import java.nio.file.Path;

public class ModelExportContext {
    DMFSceneFile scene;
    Path outputDir;
    String resourceName;

    public ModelExportContext(@NotNull String resourceName, @NotNull Path outputDir) {
        this.resourceName = resourceName;
        this.outputDir = outputDir;
        scene = new DMFSceneFile();
    }

    public ModelExportContext(@NotNull String resourceName, @NotNull Path outputDir, @NotNull DMFSceneFile scene) {
        this.resourceName = resourceName;
        this.outputDir = outputDir;
        this.scene = scene;
    }
}
