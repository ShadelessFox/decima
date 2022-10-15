package com.shade.decima.ui.data.viewer.mesh;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.viewer.mesh.dmf.DMFSceneFile;
import com.shade.util.NotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ModelExportContext {
    public int depth = 0;
    DMFSceneFile scene;
    Path outputDir;
    String resourceName;
    boolean embedBuffers;
    boolean exportTextures = false;
    boolean convertVertices = false;
    Map<RTTIObject, Integer> uuidToBufferId = new HashMap<>();

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
