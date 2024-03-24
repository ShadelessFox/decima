package com.shade.decima;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.app.Application;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.util.NotNull;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

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

        final var graph_typeHashes = graph.<long[]>get("TypeHashes");
        final var graph_typeTableData = ByteBuffer
            .wrap(graph.get("TypeTableData"))
            .position(20).slice() // skip the header
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer();
        final var graph_groups = graph.objs("Groups");
        final var graph_subGroups = graph.ints("SubGroups");
        final var graph_rootUuids = graph.objs("RootUUIDs");
        final var graph_rootIndices = graph.<int[]>get("RootIndices");
        final var graph_locatorTable = graph.objs("LocatorTable");
        final var graph_spanTable = graph.objs("SpanTable");
        final var graph_files = graph.<String[]>get("Files");

        final var group = graph_groups[48];
        final var group_numObjects = group.i32("NumObjects");
        final var group_subGroups = Arrays.copyOfRange(graph_subGroups, group.i32("SubGroupStart"), group.i32("SubGroupStart") + group.i32("SubGroupCount"));
        final var group_locators = Arrays.copyOfRange(graph_locatorTable, group.i32("LocatorStart"), group.i32("LocatorStart") + group.i32("LocatorCount"));
        final var group_roots = Arrays.copyOfRange(graph_rootUuids, group.i32("RootStart"), group.i32("RootStart") + group.i32("RootCount"));
        final var group_spans = Arrays.copyOfRange(graph_spanTable, group.i32("SpanStart"), group.i32("SpanStart") + group.i32("SpanCount"));
        final var group_types = IntStream
            .range(group.i32("TypeStart"), group.i32("TypeStart") + group.i32("TypeCount"))
            .mapToLong(index -> graph_typeHashes[graph_typeTableData.get(index) & 0xffff]) // get the hash
            .mapToObj(registry::<RTTIClass>find) // get the type
            .toList();
    }

    /**
     * Gets a byte buffer from the system clipboard.
     * <p>
     * Expects the clipboard to contain a hex string.
     */
    @NotNull
    private static ByteBuffer getBufferFromClipboard() throws Exception {
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        var text = (String) clipboard.getData(DataFlavor.stringFlavor);
        var data = HexFormat.of().parseHex(text.replaceAll("[^0-9a-fA-F]", ""));
        return ByteBuffer
            .wrap(data)
            .order(ByteOrder.LITTLE_ENDIAN);
    }
}
