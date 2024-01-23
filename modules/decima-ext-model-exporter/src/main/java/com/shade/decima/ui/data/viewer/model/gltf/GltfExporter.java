package com.shade.decima.ui.data.viewer.model.gltf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.viewer.isr.Node;
import com.shade.decima.ui.data.viewer.model.ModelExporter;
import com.shade.decima.ui.data.viewer.model.ModelExporterProvider;
import com.shade.decima.ui.data.viewer.model.isr.SceneSerializer;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;
import org.joml.Matrix4f;

import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

public class GltfExporter implements ModelExporter {
    public static class GltfProvider implements ModelExporterProvider {
        @NotNull
        @Override
        public ModelExporter create(@NotNull Project project, @NotNull Set<Option> options, @NotNull Path outputPath) {
            return new GltfExporter(project, false);
        }

        @Override
        public boolean supportsOption(@NotNull Option option) {
            return false;
        }

        @NotNull
        @Override
        public String getExtension() {
            return "gltf";
        }

        @NotNull
        @Override
        public String getName() {
            return "glTF";
        }
    }

    public static class GlbProvider implements ModelExporterProvider {
        @NotNull
        @Override
        public ModelExporter create(@NotNull Project project, @NotNull Set<Option> options, @NotNull Path outputPath) {
            return new GltfExporter(project, true);
        }

        @Override
        public boolean supportsOption(@NotNull Option option) {
            return false;
        }

        @NotNull
        @Override
        public String getExtension() {
            return "glb";
        }

        @NotNull
        @Override
        public String getName() {
            return "glTF (Binary)";
        }
    }

    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private final Project project;
    private final boolean binary;

    public GltfExporter(@NotNull Project project, boolean binary) {
        this.project = project;
        this.binary = binary;
    }

    @Override
    public void export(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull SeekableByteChannel channel
    ) throws Exception {
        final Node root = SceneSerializer.serialize(monitor, object, core, project);
        root.setMatrix(new Matrix4f().rotateX((float) Math.toRadians(-90.0)));

        if (binary) {
            GltfWriter.writeBinary(monitor, root, channel);
        } else {
            GltfWriter.writeText(monitor, root, gson.newJsonWriter(Channels.newWriter(channel, StandardCharsets.UTF_8)));
        }
    }
}
