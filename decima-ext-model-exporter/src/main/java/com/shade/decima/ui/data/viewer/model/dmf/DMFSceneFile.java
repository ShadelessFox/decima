package com.shade.decima.ui.data.viewer.model.dmf;

import com.shade.platform.model.app.Application;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DMFSceneFile {
    public final DMFSceneMetaData metadata;
    public final List<DMFCollection> collections;
    public final List<DMFNode> models;
    public final List<DMFSkeleton> skeletons;
    public final List<DMFBufferView> bufferViews;
    public final List<DMFBuffer> buffers;
    public final List<DMFMaterial> materials;
    public final List<DMFTexture> textures;
    public final List<DMFNode> instances;

    public DMFSceneFile(int version) {
        Application app = ApplicationManager.getApplication();
        metadata = new DMFSceneMetaData("%s (%s, %s)".formatted(app.getTitle(), app.getVersion(), app.getBuildNumber()), version);
        collections = new ArrayList<>();
        models = new ArrayList<>();
        skeletons = new ArrayList<>();
        bufferViews = new ArrayList<>();
        buffers = new ArrayList<>();
        materials = new ArrayList<>();
        textures = new ArrayList<>();
        instances = new ArrayList<>();
    }

    @Nullable
    public DMFMaterial getMaterial(@NotNull String materialName) {
        for (DMFMaterial material : materials) {
            if (material.name.equals(materialName)) {
                return material;
            }
        }
        return null;
    }

    @NotNull
    public DMFMaterial createMaterial(@NotNull String materialName) {
        final DMFMaterial material = new DMFMaterial(materialName);
        materials.add(material);
        return material;
    }

    @Nullable
    public DMFTexture getTexture(@NotNull String textureName) {
        for (DMFTexture texture : textures) {
            if (texture.name.equals(textureName)) {
                return texture;
            }
        }
        return null;
    }

    @NotNull
    public DMFCollection createCollection(@NotNull String name) {
        return createCollection(name, null, true);
    }

    @NotNull
    public DMFCollection createCollection(@NotNull String name, @Nullable DMFCollection parent, boolean enabled) {
        final DMFCollection collection = new DMFCollection(name, enabled, parent != null ? collections.indexOf(parent) : null);
        collections.add(collection);
        return collection;
    }

}
