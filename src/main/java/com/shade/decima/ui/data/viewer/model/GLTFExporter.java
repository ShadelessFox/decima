package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.data.viewer.model.gltf.GltfFile;
import com.shade.decima.ui.data.viewer.model.gltf.GltfScene;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public class GLTFExporter implements ModelExporter {
    public static class Provider implements ModelExporterProvider {
        @NotNull
        @Override
        public ModelExporter create(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath) {
            return new GLTFExporter(project, exportSettings, outputPath);
        }

        @NotNull
        @Override
        public String getExtension() {
            return "gltf";
        }

        @NotNull
        @Override
        public String getName() {
            return "GLTF scene";
        }
    }

    private final RTTITypeRegistry registry;
    private final PackfileManager manager;
    private final ExportSettings exportSettings;
    private final Path outputPath;

    private GltfFile file;
    private GltfScene currentScene;

    public GLTFExporter(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath) {
        registry = project.getTypeRegistry();
        manager = project.getPackfileManager();
        this.exportSettings = exportSettings;
        this.outputPath = outputPath;

    }

    @Override
    public Object export(@NotNull ProgressMonitor monitor, @NotNull CoreBinary core, @NotNull RTTIObject object, @NotNull String resourceName) throws IOException {
        file = new GltfFile();
        currentScene = file.newScene();
        file.setScene(currentScene);
        return file;
    }

}
