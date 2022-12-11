package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.data.viewer.model.gltf.GltfFile;
import com.shade.decima.ui.data.viewer.model.gltf.GltfNode;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class GLTFExporter extends ModelExporterShared implements ModelExporter {
    private static final Logger log = LoggerFactory.getLogger(GLTFExporter.class);

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

    public GLTFExporter(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath) {
        registry = project.getTypeRegistry();
        manager = project.getPackfileManager();
        this.exportSettings = exportSettings;
        this.outputPath = outputPath;

    }

    @Override
    public Object export(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        file = new GltfFile();
        file.setScene(file.newScene());
        exportResource(monitor, core, object, resourceName);
        return file;
    }

    private void exportResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        log.info("Exporting {}", object.type().getTypeName());
        switch (object.type().getTypeName()) {
//            case "ArtPartsDataResource" -> exportArtPartsDataResource(monitor, core, object, resourceName);
//            case "ArtPartsSubModelResource" -> exportArtPartsSubModelResource(monitor, core, object, resourceName);
//            case "ObjectCollection" -> exportObjectCollection(monitor, core, object, resourceName);
//            case "StaticMeshInstance" -> exportStaticMeshInstance(monitor, core, object);
//            case "Terrain" -> exportTerrainResource(monitor, core, object);
//            case "LodMeshResource" -> exportLodMeshResource(monitor, core, object, resourceName);
//            case "MultiMeshResource" -> exportMultiMeshResource(monitor, core, object, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                exportRegularSkinnedMeshResource(monitor, core, object, resourceName);
            default -> throw new IllegalArgumentException("Unsupported resource: " + object.type());
        }
    }

    private void exportRegularSkinnedMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) {
        GltfNode node = file.newNode(resourceName);

    }

}
