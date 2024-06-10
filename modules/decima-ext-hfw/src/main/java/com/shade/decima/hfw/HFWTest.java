package com.shade.decima.hfw;

import com.shade.decima.hfw.archive.StorageReadDevice;
import com.shade.decima.hfw.rtti.types.HFWTextureData;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTICoreFileReader.LoggingErrorHandlingStrategy;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTIFactory;
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
        final ProjectContainer container = project.getContainer();
        final RTTIFactory factory = project.getRTTIFactory();

        final StreamingGraphResource graph;
        try (SeekableByteChannel channel = Files.newByteChannel(expandPath(container, "cache:package/streaming_graph.core"))) {
            final RTTICoreFile file = new CoreBinary.Reader(factory).read(Channels.newInputStream(channel), LoggingErrorHandlingStrategy.getInstance());
            graph = new StreamingGraphResource(file.objects().get(0), factory);
        }

        final StorageReadDevice device = new StorageReadDevice(container);
        for (String file : graph.getFiles()) {
            device.mount(file);
        }

        final ObjectStreamingSystem system = new ObjectStreamingSystem(device, graph);
        final StreamingObjectReader reader = new StreamingObjectReader(factory, system);

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
    public static Path expandPath(@NotNull ProjectContainer project, @NotNull String path) {
        // A vague reimplementation of what they do in the engine. They access files using "file devices":
        //   SystemFileDevice - provides access to the local filesystem
        //   AliasFileDevice - maps a device path to another device path
        // In the game, they have the following file devices:
        //   source: - a system file device that points to the game's root directory
        //   cache: - an alias file device that points to a cache directory (LocalCacheWinGame) within the game's root directory (source:)

        final String[] parts = path.split(":", 2);
        return switch (parts[0]) {
            case "source" -> project.getPackfilesPath().resolve(parts[1]);
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
            oodle
        );

        final Application application = ApplicationManager.getApplication();
        final ProjectManager projectManager = application.getService(ProjectManager.class);
        projectManager.addProject(projectContainer);

        return projectManager.openProject(Objects.requireNonNull(projectContainer));
    }
}
