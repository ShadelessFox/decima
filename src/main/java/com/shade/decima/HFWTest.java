package com.shade.decima;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.app.Application;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.*;

public class HFWTest {
    public static void main(String[] args) throws Exception {
        // Replace these with the actual paths to the oodle DLL and the game "localcachepink" directory
        final Path oodle = Path.of("E:/SteamLibrary/steamapps/common/Death Stranding/oo2core_7_win64.dll");
        final Path cache = Path.of("D:/PlayStation Games/Horizon Forbidden West v1.18 Repack/Dump/localcachepink");

        final ProjectContainer projectContainer = new ProjectContainer(
            UUID.randomUUID(),
            "HFW",
            GameType.HFW,
            Path.of(""),
            cache,
            oodle,
            Path.of("data/hfw_types.json.gz"),
            null
        );

        final Application application = ApplicationManager.getApplication();
        final ProjectManager projectManager = application.getService(ProjectManager.class);
        projectManager.addProject(projectContainer);

        final Project project = projectManager.openProject(Objects.requireNonNull(projectContainer));

        final byte[] data = Files.readAllBytes(cache.resolve("package/streaming_graph.core"));
        final RTTICoreFile file = project.getCoreFileReader().read(new ByteArrayInputStream(data), true);
        final RTTIObject graph = file.objects().get(0);

        if (false) {
            final var type = project.getTypeRegistry().find("WorldDataType");
            final var buf = ByteBuffer.wrap(Files.readAllBytes(cache.resolve("package/package.00.00.core"))).order(ByteOrder.LITTLE_ENDIAN).position(0);
            final var obj = type.read(project.getTypeRegistry(), buf);
            System.out.println(obj);
            return;
        }

        if (true) {
            extractPackfile(
                project,
                graph,
                "cache:package/Initial/package.00.03.core",
                cache,
                cache.resolve("package/Initial/package.00.03.core.unpacked")
            );
        }
    }

    private static void extractPackfile(
        @NotNull Project project,
        @NotNull RTTIObject graph,
        @NotNull String name,
        @NotNull Path cache,
        @NotNull Path destination
    ) throws IOException {
        final int index = IOUtils.indexOf(graph.get("Files"), name);

        if (index < 0) {
            throw new IllegalArgumentException("Can't find packfile '" + name + "' in the graph");
        }

        final int[] offsets = graph.<int[][]>get("PackFileOffsets")[index];
        final int[] lengths = graph.<int[][]>get("PackFileLengths")[index];
        final Path packfile = cache.resolve(name.substring(6));

        try (
            SeekableByteChannel input = Files.newByteChannel(packfile, READ);
            SeekableByteChannel output = Files.newByteChannel(destination, CREATE, WRITE)
        ) {
            for (int i = 0; i < lengths.length; i++) {
                final byte[] compressed = new byte[offsets[i + 1] - offsets[i]];
                input.position(offsets[i]);
                input.read(ByteBuffer.wrap(compressed));

                final byte[] decompressed = new byte[lengths[i]];
                project.getCompressor().decompress(compressed, decompressed);

                output.write(ByteBuffer.wrap(decompressed));

                System.out.println("Decompressed chunk " + i + " of " + lengths.length + " (" + decompressed.length + " bytes)");
            }
        }
    }
}
