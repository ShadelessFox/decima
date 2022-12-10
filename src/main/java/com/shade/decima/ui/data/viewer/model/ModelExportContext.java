package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.ui.data.viewer.model.dmf.DMFCollection;
import com.shade.decima.ui.data.viewer.model.dmf.DMFSceneFile;
import com.shade.decima.ui.data.viewer.model.dmf.DMFSkeleton;
import com.shade.util.NotNull;

import java.nio.file.Path;
import java.util.Stack;

public class ModelExportContext {
    public int depth = 0;
    public DMFSceneFile scene;
    public Path outputDir;
    public String resourceName;
    public Stack<DMFCollection> collectionStack;
    public boolean embedBuffers;
    public boolean embedTextures = false;
    public boolean exportTextures = false;
    public DMFSkeleton masterSkeleton = null;

    public ModelExportContext(@NotNull String resourceName, @NotNull Path outputDir) {
        this.resourceName = resourceName;
        this.outputDir = outputDir;
        this.collectionStack = new Stack<>();
        scene = new DMFSceneFile();
    }

    public ModelExportContext(@NotNull String resourceName, @NotNull Path outputDir, @NotNull DMFSceneFile scene) {
        this.resourceName = resourceName;
        this.outputDir = outputDir;
        this.collectionStack = new Stack<>();
        this.scene = scene;
    }
}
