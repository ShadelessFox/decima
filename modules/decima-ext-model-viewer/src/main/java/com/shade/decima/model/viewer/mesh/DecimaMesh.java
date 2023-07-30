package com.shade.decima.model.viewer.mesh;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.ValueController;
import com.shade.util.NotNull;

import java.io.IOException;

public class DecimaMesh {
    private DecimaMesh() {
    }

    @NotNull
    public static Mesh create(@NotNull ValueController<RTTIObject> controller) throws IOException {
        return switch (controller.getValueType().getFullTypeName()) {
            case "ArtPartsDataResource", "ArtPartsSubModelWithChildrenResource" -> new DecimaArtPartMesh(controller);
            case "RegularSkinnedMeshResource", "StaticMeshResource" -> new DecimaSkinnedMesh(controller);
            case "LodMeshResource" -> new DecimaLodMesh(controller);
            case "MultiMeshResource" -> new DecimaMultiMesh(controller);
            default -> throw new IOException("Unsupported model type: " + controller.getValueType().getFullTypeName());
        };
    }
}
