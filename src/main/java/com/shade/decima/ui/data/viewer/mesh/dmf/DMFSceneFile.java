package com.shade.decima.ui.data.viewer.mesh.dmf;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DMFSceneFile {
    public DMFSceneMetaData metadata;
    public List<DMFCollection> collections;
    public List<DMFNode> models;
    public List<DMFSkeleton> skeletons;
    public List<DMFBuffer> buffers;
    public List<DMFMaterial> materials;
    public List<DMFTexture> textures;

    public DMFSceneFile() {
        metadata = new DMFSceneMetaData();
        collections = new ArrayList<>();
        models = new ArrayList<>();
        skeletons = new ArrayList<>();
        buffers = new ArrayList<>();
        materials = new ArrayList<>();
        textures = new ArrayList<>();
    }

    public DMFSceneFile(@NotNull String generator, int version) {
        this();
        metadata.generator = generator;
        metadata.version = version;
    }
}
