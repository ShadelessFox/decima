package com.shade.decima.hfw;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class HFWTest {
    public static void main(String[] args) throws Exception {
        // Replace these with the actual paths to the oodle DLL and the game's root directory
        final Path oodle = Path.of("E:/SteamLibrary/steamapps/common/Death Stranding/oo2core_7_win64.dll");
        final Path cache = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition");
        final Project project = createProject(cache, oodle);

        final StreamingObjectReader reader = new StreamingObjectReader(project);

        final StreamingObjectReader.ObjectResult result = reader.readObject("c3e38cdb-0163-47e9-a962-5f80f0f77669");
        final RTTIObject texture = result.object();
        final RTTIObject textureSet = texture.ref("TextureSetParent") instanceof RTTIReference.StreamingLink link ? link.getTarget() : null;

        final HwTextureHeader header = texture.obj("Header").cast();
        final HFWTextureData data = texture.obj("Data").cast();
        System.out.println("Width: " + header.getWidth() + ", Height: " + header.getHeight() + ", Format: " + header.getPixelFormat());

        final ByteBuffer bytes;
        if (data.externalDataSize > 0) {
            bytes = reader.getStreamingData(
                result.groupResult().locators().get((textureSet != null ? textureSet : texture).obj("StreamingDataSource")),
                texture.ints("StreamingMipOffsets")[0],
                data.externalDataSize
            );
        } else {
            bytes = ByteBuffer.wrap(data.internalData).order(ByteOrder.LITTLE_ENDIAN);
        }

        Files.write(Path.of("samples/hfw/texture.bin"), bytes.array());
    }

    @NotNull
    private static Project createProject(@NotNull Path root, @NotNull Path oodle) throws IOException {
        final ProjectContainer projectContainer = new ProjectContainer(
            UUID.randomUUID(),
            "HFW",
            GameType.HFW,
            root,
            root,
            oodle,
            Path.of("data/hfw_types.json.gz"),
            null
        );

        final Application application = ApplicationManager.getApplication();
        final ProjectManager projectManager = application.getService(ProjectManager.class);
        projectManager.addProject(projectContainer);
        final Project project = projectManager.openProject(Objects.requireNonNull(projectContainer));
        return project;
    }
}
