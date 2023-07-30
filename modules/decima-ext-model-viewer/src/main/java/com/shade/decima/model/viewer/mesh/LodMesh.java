package com.shade.decima.model.viewer.mesh;

import com.shade.util.NotNull;

public interface LodMesh extends Mesh {
    void setLod(@NotNull Lod lod);

    @NotNull
    Lod getLod();

    @NotNull
    Lod[] getLods();

    interface Lod {
        float getDistance();
    }
}
