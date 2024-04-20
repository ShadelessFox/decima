package com.shade.decima.hfw;

import com.shade.decima.hfw.archive.StorageReadDevice;
import com.shade.decima.hfw.rtti.types.HFWTextureData;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.java.HwTextureHeader;
import com.shade.platform.model.app.Application;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class HFWTest {
    public static void main(String[] args) throws Exception {
        // Replace these with the actual paths to the oodle DLL and the game's root directory
        final Path oodle = Path.of("E:/SteamLibrary/steamapps/common/Death Stranding/oo2core_7_win64.dll");
        final Path source = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition");
        final Project project = createProject(source, oodle);

        final RTTIObject graph;
        try (SeekableByteChannel channel = Files.newByteChannel(expandPath(project, "cache:package/streaming_graph.core"))) {
            graph = project.getCoreFileReader().read(Channels.newInputStream(channel), true).objects().get(0);
        }

        final StorageReadDevice device = new StorageReadDevice(project);
        for (String file : graph.<String[]>get("Files")) {
            device.mount(file);
        }

        final ObjectStreamingSystem system = new ObjectStreamingSystem(device, graph);
        final StreamingObjectReader reader = new StreamingObjectReader(project, system);

        final StreamingObjectReader.ObjectResult result = reader.readObject("00377119-c8e7-45d7-b37d-0f6e240c3116");
        final RTTIObject texture;
        final RTTIObject dataSource;

        if (result.object().type().isInstanceOf("UITexture")) {
            texture = result.object().obj("BigTextureData");
            dataSource = null;
        } else {
            texture = result.object();

            if (texture.ref("TextureSetParent") instanceof RTTIReference.StreamingLink link) {
                dataSource = Objects.requireNonNull(link.getTarget()).obj("StreamingDataSource");
            } else {
                dataSource = texture.obj("StreamingDataSource");
            }
        }

        final HwTextureHeader header = texture.obj("Header").cast();
        final HFWTextureData data = texture.obj("Data").cast();
        System.out.println("Width: " + header.getWidth() + ", Height: " + header.getHeight() + ", Format: " + header.getPixelFormat());

        final ByteBuffer bytes;
        if (data.externalDataSize > 0) {
            bytes = system.getDataSourceData(
                result.groupResult().locators().get(dataSource),
                texture.ints("StreamingMipOffsets")[0],
                data.externalDataSize
            );
        } else {
            bytes = ByteBuffer.wrap(data.internalData).order(ByteOrder.LITTLE_ENDIAN);
        }

        Files.write(Path.of("samples/hfw/texture.bin"), bytes.array());
    }

    @NotNull
    public static Path expandPath(@NotNull Project project, @NotNull String path) {
        // A vague reimplementation of what they do in the engine. They access files using "file devices":
        //   SystemFileDevice - provides access to the local filesystem
        //   AliasFileDevice - maps a device path to another device path
        // In the game, they have the following file devices:
        //   source: - a system file device that points to the game's root directory
        //   cache: - an alias file device that points to a cache directory (LocalCacheWinGame) within the game's root directory (source:)

        final String[] parts = path.split(":", 2);
        return switch (parts[0]) {
            case "source" -> project.getContainer().getExecutablePath().resolve(parts[1]);
            case "cache" -> expandPath(project, "source:LocalCacheWinGame/" + parts[1]);
            default -> throw new IllegalArgumentException("Unknown device path: " + path);
        };
    }

    @NotNull
    private static Project createProject(@NotNull Path source, @NotNull Path oodle) throws IOException {
        final ProjectContainer projectContainer = new ProjectContainer(
            UUID.randomUUID(),
            "HFW",
            GameType.HFW,
            source,
            source,
            oodle,
            Path.of("data/hfw_types.json.gz"),
            null
        );

        final Application application = ApplicationManager.getApplication();
        final ProjectManager projectManager = application.getService(ProjectManager.class);
        projectManager.addProject(projectContainer);

        return projectManager.openProject(Objects.requireNonNull(projectContainer));
    }
}
