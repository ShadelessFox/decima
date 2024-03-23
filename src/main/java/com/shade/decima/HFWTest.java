package com.shade.decima;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.nio.file.StandardOpenOption.*;

public class HFWTest {
    public static void main(String[] args) throws Exception {
        // Replace these with the actual paths to the oodle DLL and the game "localcachepink" directory
        final Path oodle = Path.of("E:/SteamLibrary/steamapps/common/Death Stranding/oo2core_7_win64.dll");
        final Path cache = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition/LocalCacheWinGame");

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
        final RTTITypeRegistry registry = project.getTypeRegistry();

        final byte[] data = Files.readAllBytes(cache.resolve("package/streaming_graph.core"));
        final RTTICoreFile file = project.getCoreFileReader().read(new ByteArrayInputStream(data), true);
        final RTTIObject graph = file.objects().get(0);

        if (false) {
            final var type = registry.find("WorldDataType");
            final var buf = ByteBuffer.wrap(Files.readAllBytes(cache.resolve("package/package.00.00.core"))).order(ByteOrder.LITTLE_ENDIAN).position(0);
            final var obj = type.read(registry, buf);
            System.out.println(obj);
            return;
        }

        if (false) {
            extractPackfile(
                project,
                graph,
                "cache:package/Initial/package.00.03.core",
                cache,
                cache.resolve("package/Initial/package.00.03.core.unpacked")
            );
        }

        final var graph_typeHashes = graph.<long[]>get("TypeHashes");
        final var graph_typeTableData = ByteBuffer
            .wrap(graph.get("TypeTableData"))
            .position(32).slice() // skip the header
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer();
        final var graph_groups = graph.objs("Groups");
        final var graph_subGroups = graph.ints("SubGroups");
        final var graph_rootUuids = graph.objs("RootUUIDs");
        final var graph_locatorTable = graph.objs("LocatorTable");
        final var graph_spanTable = graph.objs("SpanTable");

        // 27402 - Texture[2]
        // 59000 - LocalizedTextResource, BooleanFactValue
        final var group = graph_groups[19293];
        final var group_subGroups = Arrays.copyOfRange(graph_subGroups, group.i32("SubGroupStart"), group.i32("SubGroupStart") + group.i32("SubGroupCount"));
        final var group_locators = Arrays.copyOfRange(graph_locatorTable, group.i32("LocatorStart"), group.i32("LocatorStart") + group.i32("LocatorCount"));
        final var group_roots = Arrays.copyOfRange(graph_rootUuids, group.i32("RootStart"), group.i32("RootStart") + group.i32("RootCount"));
        final var group_spans = Arrays.copyOfRange(graph_spanTable, group.i32("SpanStart"), group.i32("SpanStart") + group.i32("SpanCount"));
        final var group_types = IntStream
            .range(group.i32("TypeStart"), group.i32("TypeStart") + group.i32("TypeCount"))
            .map(index -> graph_typeTableData.get(index) & 0xffff) // get the index in the TypeHashes array
            .mapToLong(index -> graph_typeHashes[index]) // get the actual type hash
            .mapToObj(registry::find) // get the type
            .toList();
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
