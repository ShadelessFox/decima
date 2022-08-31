package com.shade.decima.ui.data.viewer.mesh.gltf;

import java.util.ArrayList;
import java.util.List;

public class GltfFile {
    public GltfAsset asset;
    public int scene = 0;
    public final List<GltfScene> scenes = new ArrayList<>();
    public final List<GltfNode> nodes = new ArrayList<>();
    public final List<GltfMesh> meshes = new ArrayList<>();
    public final List<GltfAccessor> accessors = new ArrayList<>();
    public final List<GltfBufferView> bufferViews = new ArrayList<>();
    public final List<GltfBuffer> buffers = new ArrayList<>();
}
