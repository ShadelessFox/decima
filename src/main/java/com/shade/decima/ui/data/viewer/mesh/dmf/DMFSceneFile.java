package com.shade.decima.ui.data.viewer.mesh.dmf;

import com.shade.decima.BuildConfig;
import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.shade.decima.BuildConfig.*;

public class DMFSceneFile {
    public DMFSceneMetaData metadata;
    public List<DMFCollection> collections;
    public List<DMFNode> models;
    public List<DMFSkeleton> skeletons;
    public List<DMFBufferView> bufferViews;
    public List<DMFBuffer> buffers;
    public List<DMFMaterial> materials;
    public List<DMFTexture> textures;

    public DMFSceneFile() {
        metadata = new DMFSceneMetaData();
        collections = new ArrayList<>();
        models = new ArrayList<>();
        skeletons = new ArrayList<>();
        bufferViews = new ArrayList<>();
        buffers = new ArrayList<>();
        materials = new ArrayList<>();
        textures = new ArrayList<>();
    }

    public DMFSceneFile(@NotNull String generator, int version) {
        this();
        metadata.generator = generator;
        metadata.version = version;
    }
    public DMFSceneFile(int version) {
        this();
        metadata.generator = "%s (%s, %s)".formatted(APP_TITLE, APP_VERSION, BUILD_COMMIT);
        metadata.version = version;
    }
}
