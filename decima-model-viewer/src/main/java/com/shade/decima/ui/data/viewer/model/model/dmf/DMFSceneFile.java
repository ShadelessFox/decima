package com.shade.decima.ui.data.viewer.model.model.dmf;

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

    public DMFMaterial getMaterial(String materialName) {
        for (DMFMaterial material : materials) {
            if (material.name.equals(materialName)) {
                return material;
            }
        }
        return null;
    }

    public DMFMaterial createMaterial(String materialName) {
        DMFMaterial material = new DMFMaterial();
        material.name = materialName;
        materials.add(material);
        return material;
    }

    public DMFTexture getTexture(String textureName) {
        for (DMFTexture texture : textures) {
            if (texture.name.equals(textureName)) {
                return texture;
            }
        }
        return null;
    }

    public DMFCollection createCollection(String name) {
        return createCollection(name, null, true);
    }

    public DMFCollection createCollection(String name, DMFCollection parent) {
        return createCollection(name, parent, true);
    }

    public DMFCollection createCollection(String name, DMFCollection parent, boolean enabled) {
        DMFCollection collection = new DMFCollection(name);
        collection.enabled = enabled;
        collections.add(collection);
        if (parent != null) {
            collection.parent = collections.indexOf(parent);
        }
        return collection;
    }

}
