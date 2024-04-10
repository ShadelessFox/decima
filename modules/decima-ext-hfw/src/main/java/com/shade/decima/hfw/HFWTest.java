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
        final StreamingObjectReader.Result result = reader.readGroup(39812);

        if (true) {
            final StreamingObjectReader.GroupInfo group = result.groups().get(0);
            final RTTIObject texture = group.objects()[4];
            final RTTIObject textureSet = texture.ref("TextureSetParent") instanceof RTTIReference.StreamingLink link ? link.getTarget() : null;

            final HwTextureHeader header = texture.obj("Header").cast();
            System.out.println("Width: " + header.getWidth() + ", Height: " + header.getHeight() + ", Format: " + header.getPixelFormat());

            final ByteBuffer data = reader.getStreamingData(
                result.locators().get((textureSet != null ? textureSet : texture).obj("StreamingDataSource")),
                texture.ints("StreamingMipOffsets")[0],
                texture.obj("Data").<HFWTextureData>cast().externalDataSize
            );

            Files.write(Path.of("samples/hfw/texture.bin"), data.array());
        }
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
