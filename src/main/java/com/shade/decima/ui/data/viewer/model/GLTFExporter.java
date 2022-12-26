package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.BuildConfig;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.data.viewer.model.abstractModel.Bone;
import com.shade.decima.ui.data.viewer.model.abstractModel.Skeleton;
import com.shade.decima.ui.data.viewer.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.decima.ui.data.viewer.model.data.impl.*;
import com.shade.decima.ui.data.viewer.model.gltf.*;
import com.shade.decima.ui.data.viewer.model.utils.Matrix4x4;
import com.shade.decima.ui.data.viewer.model.utils.Quaternion;
import com.shade.decima.ui.data.viewer.model.utils.Vector3;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.shade.decima.model.rtti.messages.impl.IndexArrayResourceHandler.HwIndexArray;
import static com.shade.decima.model.rtti.messages.impl.VertexArrayResourceHandler.HwVertexArray;

public class GLTFExporter extends ModelExporterShared implements ModelExporter {
    private static final Logger log = LoggerFactory.getLogger(GLTFExporter.class);
    private static final Map<String, String> dsToGltf = Map.ofEntries(
        Map.entry("Pos", "POSITION"),
        Map.entry("Normal", "NORMAL"),
        Map.entry("Tangent", "TANGENT"),
        Map.entry("TangentBFlip", "TANGENT"),
        Map.entry("Color", "COLOR_0"),
        Map.entry("UV0", "TEXCOORD_0"),
        Map.entry("UV1", "TEXCOORD_1"),
        Map.entry("UV2", "TEXCOORD_2"),
        Map.entry("UV3", "TEXCOORD_3"),
        Map.entry("UV4", "TEXCOORD_4"),
        Map.entry("UV5", "TEXCOORD_5"),
        Map.entry("UV6", "TEXCOORD_6"),
        Map.entry("BlendIndices", "JOINTS_0"),
        Map.entry("BlendIndices2", "JOINTS_1"),
        Map.entry("BlendIndices3", "JOINTS_2"),
        Map.entry("BlendWeights", "WEIGHTS_0"),
        Map.entry("BlendWeights2", "WEIGHTS_1"),
        Map.entry("BlendWeights3", "WEIGHTS_2")
    );
    private static final Map<ComponentType, Integer> componentTypeToGltf = Map.of(
        ComponentType.INT8, 5120,
        ComponentType.UINT8, 5121,
        ComponentType.INT16, 5122,
        ComponentType.UINT16, 5123,
        ComponentType.UINT32, 5125,
        ComponentType.FLOAT32, 5126
    );

    public static class Provider implements ModelExporterProvider {
        @NotNull
        @Override
        public ModelExporter create(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath) {
            return new GLTFExporter(project, exportSettings, outputPath);
        }

        @NotNull
        @Override
        public String getExtension() {
            return "gltf";
        }

        @NotNull
        @Override
        public String getName() {
            return "GLTF scene";
        }
    }

    private final RTTITypeRegistry registry;
    private final PackfileManager manager;
    private final ExportSettings exportSettings;
    private final Path outputPath;
    private int depth = 0;
    private final Skeleton masterSkeleton = new Skeleton();
//    private final Deque<Skeleton> masterSkeletons = new ArrayDeque<>();

    private GltfFile file;

    public GLTFExporter(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath) {
        registry = project.getTypeRegistry();
        manager = project.getPackfileManager();
        this.exportSettings = exportSettings;
        this.outputPath = outputPath;

    }

    private Path getBuffersPath() throws IOException {
        Path buffersPath = outputPath.resolve("dbuffers");
        Files.createDirectories(buffersPath);
        return buffersPath;
    }

    @Override
    public Object export(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        file = new GltfFile();
        file.asset = new GltfAsset(BuildConfig.APP_TITLE + "@" + BuildConfig.BUILD_COMMIT, "2.0");
        file.setScene(file.newScene());
        exportResource(monitor, core, object, resourceName);
        return file;
    }

    private void exportResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        log.info("Exporting {}", object.type().getTypeName());
        switch (object.type().getTypeName()) {
            case "ArtPartsDataResource" -> exportArtPartsDataResource(monitor, core, object, resourceName);
//            case "ObjectCollection" -> exportObjectCollection(monitor, core, object, resourceName);
//            case "StaticMeshInstance" -> exportStaticMeshInstance(monitor, core, object);
//            case "Terrain" -> exportTerrainResource(monitor, core, object);
            case "LodMeshResource" -> exportLodMeshResource(monitor, core, object, resourceName);
            case "MultiMeshResource" -> exportMultiMeshResource(monitor, core, object, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                exportRegularSkinnedMeshResource(monitor, core, object, resourceName);
            default -> throw new IllegalArgumentException("Unsupported resource: " + object.type());
        }
    }

    private void exportArtPartsDataResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        GltfNode model;
        try (ProgressMonitor.Task artPartTask = monitor.begin("Exporting ArtPartsDataResource RootModel", 2)) {
            RTTIObject repSkeleton = object.ref("RepresentationSkeleton").follow(core, manager, registry).object();
            RTTIObject[] defaultPos = object.get("DefaultPoseTranslations");
            RTTIObject[] defaultRot = object.get("DefaultPoseRotations");
//            Skeleton skeleton = new Skeleton();
            Skeleton skeleton = masterSkeleton;
            final RTTIObject[] joints = repSkeleton.get("Joints");
            for (short i = 0; i < joints.length; i++) {
                RTTIObject joint = joints[i];
                Matrix4x4 t = Matrix4x4.Translation(new double[]{defaultPos[i].f32("X"), defaultPos[i].f32("Y"), defaultPos[i].f32("Z")});
                Matrix4x4 r = new Quaternion(new double[]{defaultRot[i].f32("X"), defaultRot[i].f32("Y"), defaultRot[i].f32("Z"), defaultRot[i].f32("W")}).toMatrix().to4x4();
                final Matrix4x4 matrix = t.matMul(r);
                final short parentIndex = joint.i16("ParentIndex");
                Bone bone;
                if (parentIndex == -1) {
                    bone = skeleton.addBone(joint.str("Name"), matrix);
                } else {
                    bone = skeleton.addBone(joint.str("Name"), matrix, skeleton.getBoneId(joints[parentIndex].str("Name")));
                }
                bone.isRelative = true;
            }
//            masterSkeletons.push(skeleton);


            try (ProgressMonitor.Task task = artPartTask.split(1).begin("Exporting RootModel", 1)) {
                RTTIReference.FollowResult rootModelRes = object.ref("RootModel").follow(core, manager, registry);
                model = toModel(task.split(1), rootModelRes.binary(), rootModelRes.object(), nameFromReference(object.ref("RootModel"), resourceName));
            }
            RTTIReference[] subModels = object.get("SubModelPartResources");
            try (ProgressMonitor.Task task = artPartTask.split(1).begin("Exporting SubModelPartResources", subModels.length)) {
                for (int i = 0; i < subModels.length; i++) {
                    RTTIReference subPart = subModels[i];
                    RTTIReference.FollowResult subPartRes = subPart.follow(core, manager, registry);
                    GltfNode node = toModel(task.split(1), subPartRes.binary(), subPartRes.object(), "SubModel%d_%s".formatted(i, nameFromReference(subPart, resourceName)));
                    file.addChild(model, node);
                }
            }
        }
//        masterSkeletons.pop();
        file.addToScene(model);
    }


    private void exportObjectCollection(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        GltfNode group = file.newNode("COLLECTION_ROOT");
        int itemId = 0;
        RTTIReference[] objects = object.get("Objects");
        try (ProgressMonitor.Task task = monitor.begin("Exporting ObjectCollection Objects", objects.length)) {
            for (RTTIReference rttiReference : objects) {
                RTTIReference.FollowResult refObject = rttiReference.follow(core, manager, registry);
                GltfNode node = toModel(task.split(1), refObject.binary(), refObject.object(), nameFromReference(rttiReference, "%s_Object_%d".formatted(resourceName, itemId)));
                file.addChild(group, node);
                itemId++;
            }
        }
        file.addToScene(group);
    }


    private void exportLodMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        GltfNode model;
        try (ProgressMonitor.Task task = monitor.begin("Exporting LodMeshResource mesh", 1)) {
            model = lodMeshResourceToModel(task.split(1), core, object, resourceName);
        }
        file.addToScene(model);
    }

    private void exportMultiMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object, @NotNull String resourceName
    ) throws Throwable {
        GltfNode model;
        try (ProgressMonitor.Task task = monitor.begin("Exporting MultiMeshResource mesh", 1)) {
            model = multiMeshResourceToModel(task.split(1), core, object, resourceName);
        }
        file.addToScene(model);
    }

    private void exportRegularSkinnedMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        GltfNode model;
        try (ProgressMonitor.Task task = monitor.begin("Exporting RegularSkinnedMeshResource mesh", 1)) {
            model = regularSkinnedMeshResourceToModel(task.split(1), core, object, resourceName);
        }
        file.addToScene(model);
    }

    private GltfNode toModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        depth += 1;
        log.info("{}Converting {}", "\t".repeat(depth), object.type().getTypeName());
        var res = switch (object.type().getTypeName()) {
            case "PrefabResource" -> prefabResourceToModel(monitor, core, object, resourceName);
            case "ModelPartResource" -> modelPartResourceToModel(monitor, core, object, resourceName);
            case "ArtPartsSubModelWithChildrenResource" ->
                artPartsSubModelWithChildrenResourceToModel(monitor, core, object, resourceName);
            case "ArtPartsSubModelResource" -> artPartsSubModelResourceToModel(monitor, core, object, resourceName);
            case "PrefabInstance" -> prefabInstanceToModel(monitor, core, object, resourceName);
            case "ObjectCollection" -> objectCollectionToModel(monitor, core, object, resourceName);
            case "StaticMeshInstance" -> staticMeshInstanceToModel(monitor, core, object, resourceName);
//            case "Terrain" -> terrainResourceToModel(monitor,core, object);
            case "LodMeshResource" -> lodMeshResourceToModel(monitor, core, object, resourceName);
            case "MultiMeshResource" -> multiMeshResourceToModel(monitor, core, object, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                regularSkinnedMeshResourceToModel(monitor, core, object, resourceName);
            default -> {
                log.info("{}Cannot export {}", "\t".repeat(depth), object.type().getTypeName());
                yield null;
            }
        };
        depth -= 1;
        return res;
    }

    private GltfNode prefabResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        RTTIReference objectCollection = object.ref("ObjectCollection");
        RTTIReference.FollowResult prefabResource = objectCollection.follow(core, manager, registry);
        try (ProgressMonitor.Task task = monitor.begin("Exporting PrefabResource ObjectCollection", 1)) {
            return toModel(task.split(1), prefabResource.binary(), prefabResource.object(), nameFromReference(objectCollection, resourceName));
        }
    }

    private GltfNode modelPartResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        RTTIReference meshResourceRef = object.ref("MeshResource");
        RTTIReference.FollowResult meshResource = meshResourceRef.follow(core, manager, registry);
        try (ProgressMonitor.Task task = monitor.begin("Exporting ModelPartResource MeshResource", 1)) {
            return toModel(task.split(1), meshResource.binary(), meshResource.object(), nameFromReference(meshResourceRef, resourceName));
        }
    }

    private GltfNode artPartsSubModelWithChildrenResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        RTTIReference meshResourceRef = object.ref("ArtPartsSubModelPartResource");
        GltfNode model;
        if (meshResourceRef.type() != RTTIReference.Type.NONE) {
            try (ProgressMonitor.Task artPartTask = monitor.begin("Exporting ArtPartsSubModelWithChildrenResource", 1)) {
                RTTIReference.FollowResult meshResourceRes = meshResourceRef.follow(core, manager, registry);
                model = toModel(artPartTask.split(1), meshResourceRes.binary(), meshResourceRes.object(), nameFromReference(meshResourceRef, resourceName));
            }
        } else {
            model = file.newNode(resourceName);
        }
        RTTIReference[] children = object.get("Children");
        if (children.length > 0) {
            try (ProgressMonitor.Task task = monitor.begin("Exporting Children", children.length)) {
                for (int i = 0; i < children.length; i++) {
                    RTTIReference subPart = children[i];
                    RTTIReference.FollowResult subPartRes = subPart.follow(core, manager, registry);
                    GltfNode node = toModel(task.split(1), subPartRes.binary(), subPartRes.object(), nameFromReference(subPart, "child%d_%s".formatted(i, resourceName)));
                    file.addChild(model, node);
                }
            }
        }

        return model;
    }

    private GltfNode artPartsSubModelResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        RTTIReference meshResourceRef = object.ref("MeshResource");
        GltfNode model;
//        boolean hasSkeleton = false;
        if (object.ref("ExtraResource").type() != RTTIReference.Type.NONE) {
            RTTIReference.FollowResult extraResourceRef = object.ref("ExtraResource").follow(core, manager, registry);
            if (extraResourceRef.object().type().getTypeName().equals("ArtPartsCoverModelResource") |
                extraResourceRef.object().type().getTypeName().equals("ArtPartsCoverAndAnimResource")) {
                RTTIObject repSkeleton = object.ref("Skeleton").follow(core, manager, registry).object();
                RTTIObject[] defaultPos = extraResourceRef.object().get("DefaultPoseTranslations");
                RTTIObject[] defaultRot = extraResourceRef.object().get("DefaultPoseRotations");
                Skeleton skeleton = new Skeleton();
                final RTTIObject[] joints = repSkeleton.get("Joints");
                for (short i = 0; i < joints.length; i++) {
                    RTTIObject joint = joints[i];
                    Matrix4x4 t = Matrix4x4.Translation(new double[]{defaultPos[i].f32("X"), defaultPos[i].f32("Y"), defaultPos[i].f32("Z")});
                    Matrix4x4 r = new Quaternion(new double[]{defaultRot[i].f32("X"), defaultRot[i].f32("Y"), defaultRot[i].f32("Z"), defaultRot[i].f32("W")}).toMatrix().to4x4();
                    final Matrix4x4 matrix = t.matMul(r);
                    final short parentIndex = joint.i16("ParentIndex");
                    Bone bone;
                    if (parentIndex == -1) {
                        bone = skeleton.addBone(joint.str("Name"), matrix);
                    } else {
                        bone = skeleton.addBone(joint.str("Name"), matrix, skeleton.getBoneId(joints[parentIndex].str("Name")));
                    }
                    bone.isRelative = true;
                }
//                masterSkeletons.push(skeleton);
//                hasSkeleton = true;
            }
        }
        if (meshResourceRef.type() != RTTIReference.Type.NONE) {
            RTTIReference.FollowResult meshResourceRes = meshResourceRef.follow(core, manager, registry);
            try (ProgressMonitor.Task task = monitor.begin("Exporting ArtPartsSubModelResource MeshResource", 1)) {
                model = toModel(task.split(1), meshResourceRes.binary(), meshResourceRes.object(), nameFromReference(meshResourceRef, resourceName));
            }
        } else {
            model = file.newNode(resourceName);
        }

        RTTIReference extraMeshResourceRef = object.ref("ExtraResource");
        if (extraMeshResourceRef.type() != RTTIReference.Type.NONE) {
            RTTIReference.FollowResult extraMeshResourceRes = extraMeshResourceRef.follow(core, manager, registry);
            GltfNode extraModel;
            try (ProgressMonitor.Task task = monitor.begin("Exporting ArtPartsSubModelResource ExtraResource", 1)) {
                extraModel = toModel(task.split(1), extraMeshResourceRes.binary(), extraMeshResourceRes.object(), "EXTRA_" + nameFromReference(extraMeshResourceRef, resourceName));
            }
            if (extraModel != null) {
                file.addChild(model, extraModel);
            }
        }
//        if (hasSkeleton)
//            masterSkeletons.pop();
        return model;
    }

    private GltfNode prefabInstanceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        RTTIReference prefab = object.ref("Prefab");
        RTTIReference.FollowResult prefabResource = prefab.follow(core, manager, registry);
        resourceName = nameFromReference(prefab, resourceName);
        GltfNode prefabRoot = file.newNode(resourceName + "_PREFAB");
        GltfNode node;
        try (ProgressMonitor.Task task = monitor.begin("Exporting PrefabInstance Prefab", 1)) {
            node = toModel(task.split(1), prefabResource.binary(), prefabResource.object(), resourceName);
        }
        if (node == null) {
            return null;
        }
        if (node.matrix != null) {
            throw new IllegalStateException("Unexpected transform");
        }
        Matrix4x4 transform = worldTransformToMatrix(object.get("Orientation"));
        prefabRoot.matrix = transform.toArray();
        file.addChild(prefabRoot, node);
        return prefabRoot;
    }

    private GltfNode objectCollectionToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        RTTIReference[] objects = object.get("Objects");
        GltfNode group = file.newNode("Collection %s".formatted(resourceName));
        int itemId = 0;
        try (ProgressMonitor.Task task = monitor.begin("Exporting ObjectCollection Objects", objects.length)) {
            for (RTTIReference rttiReference : objects) {
                RTTIReference.FollowResult refObject = rttiReference.follow(core, manager, registry);
                GltfNode node = toModel(task.split(1), refObject.binary(), refObject.object(), "%s_Object_%d".formatted(nameFromReference(rttiReference, resourceName), itemId));
                itemId++;
                if (node == null) {
                    continue;
                }
                file.addChild(group, node);
            }
        }
        return group;
    }

    private GltfNode staticMeshInstanceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        RTTIReference resource = object.ref("Resource");
        RTTIReference.FollowResult meshResource = resource.follow(core, manager, registry);
        try (ProgressMonitor.Task task = monitor.begin("Exporting StaticMeshInstance Resource", 1)) {
            return toModel(task.split(1), meshResource.binary(), meshResource.object(), nameFromReference(resource, resourceName));
        }
    }

    private GltfNode lodMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        RTTIObject[] meshes = object.get("Meshes");
        if (meshes.length == 0) {
            return null;
        }
        RTTIObject lod = meshes[0];
        RTTIReference meshRef = lod.ref("Mesh");
        final var mesh = meshRef.follow(core, manager, registry);
        try (ProgressMonitor.Task task = monitor.begin("Exporting LodMeshResource LOD0", 1)) {
            return toModel(task.split(1), mesh.binary(), mesh.object(), "%s_LOD%d".formatted(nameFromReference(meshRef, resourceName), 0));
        }
    }

    private GltfNode multiMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        GltfNode group = file.newNode(resourceName + "_GROUP");
        RTTIObject[] parts = object.get("Parts");
        try (ProgressMonitor.Task task = monitor.begin("Exporting MultiMeshResource Parts", parts.length)) {
            for (int partId = 0; partId < parts.length; partId++) {
                RTTIObject part = parts[partId];
                RTTIReference meshRef = part.ref("Mesh");
                final var mesh = meshRef.follow(core, manager, registry);
                Matrix4x4 transform = worldTransformToMatrix(part.obj("Transform"));
                GltfNode model = toModel(task.split(1), mesh.binary(), mesh.object(), "%s_Part%d".formatted(nameFromReference(meshRef, resourceName), partId));
                if (model == null) continue;
                if (model.matrix != null) {
                    throw new IllegalStateException("Model already had transforms, please handle me!");
                }
                model.matrix = transform.toArray();
                file.addChild(group, model);
            }
        }
        return group;
    }


    private GltfNode regularSkinnedMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {

        Map<Short, Short> boneRemapTable = new HashMap<>();
        final RTTIObject skeletonObj = object.ref("Skeleton").follow(core, manager, registry).object();
        final RTTIObject meshJointBindings = object.ref("SkinnedMeshJointBindings").follow(core, manager, registry).object();
        final RTTIObject[] joints = skeletonObj.get("Joints");
        final short[] jointIndexList = meshJointBindings.get("JointIndexList");
        final RTTIObject[] inverseBindMatrices = meshJointBindings.get("InverseBindMatrices");

        GltfNode skeletonNode = file.newNode(resourceName + "_SKELETON");

        GltfSkin skin = file.newSkin(skeletonNode.name);
        final GltfMesh gltfMesh = new GltfMesh(file);
        final GltfNode model = file.newNode(resourceName, gltfMesh);
        file.addChild(skeletonNode, model);
        file.setSkin(model, skin);

//        final Skeleton masterSkeleton = masterSkeletons.peekFirst();
//        final Skeleton skeleton = new Skeleton();
        Skeleton skeleton = masterSkeleton;


        for (short i = 0; i < joints.length; i++) {
            int localBoneId = IOUtils.indexOf(jointIndexList, i);
            if (localBoneId == -1) {
                Bone bone;
                if (masterSkeleton != null) {
                    Bone masterBone = masterSkeleton.findByName(joints[i].str("Name"));
                    if (masterBone == null)
                        continue;
                    if (masterBone.parent == -1) {
                        bone = skeleton.addBone(masterBone.name, masterBone.matrix);
                    } else {
                        bone = skeleton.addBone(masterBone.name, masterBone.matrix, skeleton.getBoneId(masterSkeleton.get(masterBone.parent).name));
                    }
                    bone.isRelative = masterBone.isRelative;
                }
                continue;
            }

            RTTIObject joint = joints[i];
            Matrix4x4 matrix = null;
            boolean isRelative = false;
            if (masterSkeleton != null) {
                Bone masterBone = masterSkeleton.findByName(joints[i].str("Name"));
                if (masterBone != null) {
                    matrix = masterBone.matrix;
                    isRelative = true;
                }
            }
            if (matrix == null) {
                matrix = InvertedMatrix4x4TransformToMatrix(inverseBindMatrices[localBoneId]).inverted();
                matrix = Matrix4x4.Translation(matrix.toTranslation().mul(new Vector3(1, 1, 1))).matMul(matrix.toQuaternion().toMatrix().to4x4());
            }
            Bone bone;
            final short parentIndex = joint.i16("ParentIndex");
            if (parentIndex == -1) {
                bone = skeleton.addBone(joint.str("Name"), matrix);
            } else {
                final int parentBoneId = skeleton.getBoneId(joints[parentIndex].str("Name"));
                if (parentBoneId == -1) {
                    bone = skeleton.addBone(joint.str("Name"), matrix);
                } else {
                    bone = skeleton.addBone(joint.str("Name"), matrix, parentBoneId);
                }
            }
            bone.inverseBindMatrix = InvertedMatrix4x4TransformToMatrix(inverseBindMatrices[localBoneId]);
            bone.isRelative = isRelative;
        }
        short[] bones = new short[skeleton.getBones().size()];
        List<Bone> skeletonBones = skeleton.getBones();

        ByteBuffer invBindMatrixBuffer = ByteBuffer.allocate(16 * 4 * skeletonBones.size()).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < skeletonBones.size(); i++) {
            Bone bone = skeletonBones.get(i);
            GltfNode node = file.getNodeByName(bone.name);

            final Matrix4x4 inverted = skeleton.getInverseBindMatrix(bone).transposed();
            for (double v : inverted.toArray()) {
                invBindMatrixBuffer.putFloat((float) v);
            }
            if (node == null) {
                if (bone.parent == -1) {
                    node = file.newNode(bone.name, skeletonNode);
                    skin.skeleton = file.nodes.indexOf(node);
                } else {
                    node = file.newNode(bone.name, file.getNodeByName(skeleton.get(bone.parent).name));
                }
                final Matrix4x4 transposed = skeleton.toRelative(bone).transposed();
                node.matrix = transposed.toArray();
            }
            bones[i] = file.addBone(skin, node);
        }
        GltfAccessor inverseBindMatricesBuffer = genericGltfBuffer("inverseBindMatrices", invBindMatrixBuffer.position(0), ElementType.MAT4, componentTypeToGltf.get(ComponentType.FLOAT32), skeletonBones.size(), false, exportSettings.embedBuffers);
        skin.inverseBindMatrices = file.accessors.indexOf(inverseBindMatricesBuffer);

        for (short targetId : jointIndexList) {
            RTTIObject targetBone = joints[targetId];
            boneRemapTable.put(targetId, bones[skeleton.getBoneId(targetBone.str("Name"))]);
        }


        final String dataSourceLocation = "%s.core.stream".formatted(object.obj("DataSource").str("Location"));
        final Packfile dataSourcePackfile = Objects.requireNonNull(manager.findAny(dataSourceLocation), "Can't find referenced data source");
        final ByteBuffer dataSource = ByteBuffer
            .wrap(dataSourcePackfile.extract(dataSourceLocation))
            .order(ByteOrder.LITTLE_ENDIAN);


        int dataSourceOffset = 0;
        Map<RTTIObject, Map.Entry<Integer, Integer>> bufferOffsets = new HashMap<>();
        final RTTIReference[] primitivesRefs = object.get("Primitives");

        try (ProgressMonitor.Task task = monitor.begin("Exporting RegularSkinnedMeshResource primitives", 2)) {
            try (ProgressMonitor.Task collectTask = task.split(1).begin("Collecting primitives", primitivesRefs.length)) {
                for (RTTIReference primitivesRef : primitivesRefs) {
                    final var primitiveRes = primitivesRef.follow(core, manager, registry);
                    final RTTIObject primitiveObj = primitiveRes.object();
                    RTTIObject vertexArray = primitiveObj.ref("VertexArray").follow(primitiveRes.binary(), manager, registry).object();
                    RTTIObject indexArray = primitiveObj.ref("IndexArray").follow(primitiveRes.binary(), manager, registry).object();
                    final var vertices = vertexArray.obj("Data");
                    final var indices = indexArray.obj("Data");

                    final int vertexCount = vertices.i32("VertexCount");
                    final int indexCount = indices.i32("IndexCount");

                    RTTIObject vertexArrayUUID = vertexArray.get("ObjectUUID");
                    if (!bufferOffsets.containsKey(vertexArrayUUID)) {
                        bufferOffsets.put(vertexArrayUUID, Map.entry(dataSourceOffset, bufferOffsets.size()));
                        for (RTTIObject stream : vertices.<RTTIObject[]>get("Streams")) {
                            final int stride = stream.i32("Stride");
                            dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
                        }
                    }
                    RTTIObject indicesArrayUUID = indexArray.get("ObjectUUID");
                    if (!bufferOffsets.containsKey(indicesArrayUUID)) {
                        bufferOffsets.put(indicesArrayUUID, Map.entry(dataSourceOffset, bufferOffsets.size()));
                        int indexSize = switch (indices.str("Format")) {
                            case "Index16" -> 2;
                            case "Index32" -> 4;
                            default -> throw new IllegalStateException("Unexpected value: " + indices.str("Format"));
                        };

                        dataSourceOffset += IOUtils.alignUp(indexSize * indexCount, 256);
                    }
                    collectTask.worked(1);

                }
            }
            for (RTTIReference ref : primitivesRefs) {
                final var primitiveRes = ref.follow(core, manager, registry);
                final RTTIObject primitiveObj = primitiveRes.object();
                final RTTIObject vertexArray = primitiveObj.ref("VertexArray").follow(primitiveRes.binary(), manager, registry).object();
                final RTTIObject indexArray = primitiveObj.ref("IndexArray").follow(primitiveRes.binary(), manager, registry).object();
                final HwVertexArray vertices = vertexArray.obj("Data").cast();
                final HwIndexArray indices = indexArray.obj("Data").cast();

                RTTIObject vertexArrayUUID = vertexArray.get("ObjectUUID");
                RTTIObject indicesArrayUUID = indexArray.get("ObjectUUID");

                Map.Entry<Integer, Integer> offsetAndGroupId = bufferOffsets.get(vertexArrayUUID);
                dataSourceOffset = offsetAndGroupId.getKey();

                final int vertexCount = vertices.vertexCount;
                final int indexCount = indices.indexCount;
                final int indexStartIndex = primitiveObj.i32("StartIndex");
                final int indexEndIndex = primitiveObj.i32("EndIndex");

                final Map<String, Accessor> attributes = new HashMap<>();
                int uvLayerCount = 0;
                for (RTTIObject stream : vertices.streams) {
                    final int stride = stream.i32("Stride");

                    for (RTTIObject element : stream.<RTTIObject[]>get("Elements")) {
                        final int offset = element.i8("Offset");

                        final var elementType = switch (element.str("Type")) {
                            case "Pos", "Normal" -> ElementType.VEC3;
                            case "UV0", "UV1", "UV2", "UV3", "UV4", "UV5", "UV6" -> ElementType.VEC2;
                            case "BlendIndices", "BlendIndices2", "BlendIndices3",
                                "BlendWeights", "BlendWeights2", "BlendWeights3", "Color", "Tangent", "TangentBFlip" ->
                                ElementType.VEC4;
                            default ->
                                throw new IllegalArgumentException("Unsupported element type: " + element.str("Type"));
                        };

                        final var semanticName = dsToGltf.get(element.str("Type"));

                        final var accessor = switch (element.str("StorageType")) {
                            case "UnsignedByte", "UnsignedByteNormalized" ->
                                new ByteAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride, true);
                            case "SignedShort", "SignedShortNormalized" ->
                                new ShortAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride, false);
                            case "UnsignedShort", "UnsignedShortNormalized" ->
                                new ShortAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride, true);
                            case "HalfFloat" ->
                                new HalfFloatAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride);
                            case "Float" ->
                                new FloatAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride);
                            case "X10Y10Z10W2Normalized" ->
                                new X10Y10Z10W2NormalizedAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride);
                            case "X10Y10Z10W2UNorm" ->
                                new X10Y10Z10W2UNormAccessor(dataSource, ElementType.SCALAR, vertexCount, dataSourceOffset + offset, stride);
                            default ->
                                throw new IllegalArgumentException("Unsupported component type: " + element.str("StorageType"));
                        };

                        attributes.put(semanticName, accessor);
                    }

                    dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
                }


                final GltfMesh.Primitive gltfMeshPrimitive = new GltfMesh.Primitive(gltfMesh);

                for (Map.Entry<String, Accessor> entry : attributes.entrySet()) {
                    String semantic = entry.getKey();
                    Accessor supplier = entry.getValue();
                    final ByteBuffer buffer;
                    final Accessor consumer;
                    final ComponentType componentType;
                    final ElementType elementType;
                    boolean normalized = false;
                    if (semantic.equals("TANGENT")) {
                        continue;
                    }
                    if (semantic.contains("WEIGHTS_") || semantic.contains("JOINTS_")) {
                        continue;
                    }
                    switch (semantic) {
                        case "POSITION", "NORMAL" -> {
                            componentType = ComponentType.FLOAT32;
                            elementType = ElementType.VEC3;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new FloatAccessor(buffer, elementType, vertexCount, 0, 12);
                        }
                        case "WEIGHTS_0", "WEIGHTS_1", "WEIGHTS_2", "WEIGHTS_3" -> {
                            componentType = ComponentType.FLOAT32;
                            elementType = ElementType.VEC4;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new FloatAccessor(buffer, elementType, vertexCount, 0, 16);
                        }
                        case "COLOR_0" -> {
                            normalized = true;
                            componentType = ComponentType.UINT8;
                            elementType = ElementType.VEC4;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new ByteAccessor(buffer, elementType, vertexCount, 0, 4, true);
                        }
                        case "JOINTS_0", "JOINTS_1", "JOINTS_2", "JOINTS_3" -> {
                            componentType = ComponentType.UINT16;
                            elementType = ElementType.VEC4;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new ShortAccessor(buffer, elementType, vertexCount, 0, 8, true);
                        }
                        case "TEXCOORD_0", "TEXCOORD_1", "TEXCOORD_2", "TEXCOORD_3", "TEXCOORD_4", "TEXCOORD_5", "TEXCOORD_6" -> {
                            componentType = ComponentType.FLOAT32;
                            elementType = ElementType.VEC2;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new FloatAccessor(buffer, elementType, vertexCount, 0, 8);
                        }
                        default -> throw new IllegalArgumentException("Unsupported semantic: " + semantic);
                    }
                    AbstractAccessor.transfer(supplier, consumer);

                    final GltfAccessor gltfAccessor = genericGltfBuffer(semantic, buffer, elementType, componentTypeToGltf.get(componentType), vertexCount, normalized, exportSettings.embedBuffers);
                    if (semantic.equals("POSITION")) {
                        float mnX = 2e+31f, mnY = 2e+31f, mnZ = 2e+31f,
                            mxX = -2e+31f, mxY = -2e+31f, mxZ = -2e+31f;
                        FloatAccessor positionAccessor = (FloatAccessor) consumer;
                        for (int i = 0; i < vertexCount; i++) {
                            float x = positionAccessor.get(i, 0);
                            float y = positionAccessor.get(i, 1);
                            float z = positionAccessor.get(i, 2);
                            mnX = Math.min(mnX, x);
                            mnY = Math.min(mnY, y);
                            mnZ = Math.min(mnZ, z);

                            mxX = Math.max(mxX, x);
                            mxY = Math.max(mxY, y);
                            mxZ = Math.max(mxZ, z);
                        }
                        gltfAccessor.min = new double[]{mnX, mnY, mnZ};
                        gltfAccessor.max = new double[]{mxX, mxY, mxZ};
                    }
                    if (semantic.contains("TEXCOORD_")) {
                        semantic = "TEXCOORD_%d".formatted(uvLayerCount);
                        uvLayerCount++;
                    }
                    gltfMeshPrimitive.attributes.put(semantic, file.accessors.indexOf(gltfAccessor));
                }


                int usedWeightsAttributes = 0;
                for (int i = 0; i < 4; i++) {
                    if (attributes.containsKey("WEIGHTS_%d".formatted(i)))
                        usedWeightsAttributes++;
                }
                if (usedWeightsAttributes > 0) {
                    float[][] boneWeights = new float[vertexCount][usedWeightsAttributes * 4 + 1];
                    short[][] boneIndices = new short[vertexCount][usedWeightsAttributes * 4];
                    int wightsBufferCount = 0;
                    int indexBufferCount = 0;
                    for (int i = 0; i < 4; i++) {
                        final String weightsKey = "WEIGHTS_%d".formatted(i);
                        if (attributes.containsKey(weightsKey)) {
                            Accessor weightsAccessor = attributes.get(weightsKey);
                            for (int elemIndex = 0; elemIndex < vertexCount; elemIndex++) {
                                for (int compIndex = 0; compIndex < 4; compIndex++) {
                                    float item = FloatAccessor.get(weightsAccessor, elemIndex, compIndex);
                                    boneWeights[elemIndex][wightsBufferCount * 4 + compIndex + 1] = item;
                                }
                            }
                            wightsBufferCount++;
                        }
                        final String indicesKey = "JOINTS_%d".formatted(i);
                        if (attributes.containsKey(indicesKey)) {
                            Accessor indicesAccessor = attributes.get(indicesKey);
                            for (int elemIndex = 0; elemIndex < vertexCount; elemIndex++) {
                                for (int compIndex = 0; compIndex < 4; compIndex++) {
                                    short item = ShortAccessor.get(indicesAccessor, elemIndex, compIndex);
                                    Short remapped = boneRemapTable.get(item);
                                    if (remapped == null)
                                        remapped = 0;
                                    boneIndices[elemIndex][indexBufferCount * 4 + compIndex] = remapped;
                                }
                            }
                            indexBufferCount++;
                        }
                    }
                    for (int elemIndex = 0; elemIndex < vertexCount; elemIndex++) {
                        float sum = 0.f;
                        for (int compIndex = 0; compIndex < usedWeightsAttributes * 4 + 1; compIndex++) {
                            final float weight = boneWeights[elemIndex][compIndex];
                            sum += weight;
                        }
                        boneWeights[elemIndex][0] = 1.f - sum;
                    }

                    for (int bufferId = 0; bufferId < usedWeightsAttributes; bufferId++) {
                        ElementType elementType = ElementType.VEC4;
                        ByteBuffer weightBuffer = ByteBuffer.allocate(elementType.getStride(ComponentType.FLOAT32) * vertexCount).order(ByteOrder.LITTLE_ENDIAN);
                        ByteBuffer indicesBuffer = ByteBuffer.allocate(elementType.getStride(ComponentType.UINT16) * vertexCount).order(ByteOrder.LITTLE_ENDIAN);
                        for (int vertexId = 0; vertexId < vertexCount; vertexId++) {
                            for (int i = 0; i < 4; i++) {
                                final float weight = boneWeights[vertexId][i + bufferId * 4];
                                weightBuffer.putFloat(weight);
                                if (weight == 0)
                                    indicesBuffer.putShort((short) 0);
                                else
                                    indicesBuffer.putShort(boneIndices[vertexId][i + bufferId * 4]);
                            }
                        }
                        final String weightsSemantic = "WEIGHTS_%d".formatted(bufferId);
                        final String jointsSemantic = "JOINTS_%d".formatted(bufferId);
                        GltfAccessor weightGltfBuffer = genericGltfBuffer(weightsSemantic, weightBuffer, elementType, componentTypeToGltf.get(ComponentType.FLOAT32), vertexCount, false, exportSettings.embedBuffers);
                        GltfAccessor indicesGltfBuffer = genericGltfBuffer(jointsSemantic, indicesBuffer, elementType, componentTypeToGltf.get(ComponentType.UINT16), vertexCount, false, exportSettings.embedBuffers);

                        gltfMeshPrimitive.attributes.put(weightsSemantic, file.accessors.indexOf(weightGltfBuffer));
                        gltfMeshPrimitive.attributes.put(jointsSemantic, file.accessors.indexOf(indicesGltfBuffer));
                    }
                } else {
                    short[][] boneIndices = new short[vertexCount][4];

                    final String indicesKey = "JOINTS_0";
                    if (attributes.containsKey(indicesKey)) {
                        Accessor indicesAccessor = attributes.get(indicesKey);
                        for (int elemIndex = 0; elemIndex < vertexCount; elemIndex++) {
                            for (int compIndex = 0; compIndex < 4; compIndex++) {
                                short item = ShortAccessor.get(indicesAccessor, elemIndex, compIndex);
                                Short remapped = boneRemapTable.get(item);
                                if (remapped == null)
                                    remapped = 0;
                                boneIndices[elemIndex][compIndex] = remapped;
                            }
                        }
                    }

                    ElementType elementType = ElementType.VEC4;
                    ByteBuffer weightBuffer = ByteBuffer.allocate(elementType.getStride(ComponentType.FLOAT32) * vertexCount).order(ByteOrder.LITTLE_ENDIAN);
                    ByteBuffer indicesBuffer = ByteBuffer.allocate(elementType.getStride(ComponentType.UINT16) * vertexCount).order(ByteOrder.LITTLE_ENDIAN);
                    for (int vertexId = 0; vertexId < vertexCount; vertexId++) {
                        weightBuffer.putFloat(1);
                        indicesBuffer.putShort(boneIndices[vertexId][0]);
                        for (int i = 0; i < 3; i++) {
                            weightBuffer.putFloat(0);
                            indicesBuffer.putShort((short) 0);
                        }
                    }
                    final String weightsSemantic = "WEIGHTS_0";
                    final String jointsSemantic = "JOINTS_0";
                    GltfAccessor weightGltfBuffer = genericGltfBuffer(weightsSemantic, weightBuffer, elementType, componentTypeToGltf.get(ComponentType.FLOAT32), vertexCount, false, exportSettings.embedBuffers);
                    GltfAccessor indicesGltfBuffer = genericGltfBuffer(jointsSemantic, indicesBuffer, elementType, componentTypeToGltf.get(ComponentType.UINT16), vertexCount, false, exportSettings.embedBuffers);

                    gltfMeshPrimitive.attributes.put(weightsSemantic, file.accessors.indexOf(weightGltfBuffer));
                    gltfMeshPrimitive.attributes.put(jointsSemantic, file.accessors.indexOf(indicesGltfBuffer));
                }
                offsetAndGroupId = bufferOffsets.get(indicesArrayUUID);
                final var indicesBuffer = dataSource.slice(offsetAndGroupId.getKey(), indexCount * indices.getIndexSize());


                final GltfBuffer indicesGltfBuffer = new GltfBuffer(file);
                indicesGltfBuffer.uri = getUri(indicesBuffer);
                indicesGltfBuffer.byteLength = indicesBuffer.capacity();

                final GltfBufferView indicesBufferView = new GltfBufferView(file, indicesGltfBuffer);
                indicesBufferView.byteOffset = 0;
                indicesBufferView.byteLength = indicesBuffer.capacity();

                final GltfAccessor indicesGltfAccessor = new GltfAccessor(file, indicesBufferView);
                indicesGltfAccessor.type = ElementType.SCALAR.name();
                indicesGltfAccessor.componentType = componentTypeToGltf.get(indices.getIndexSize() == 2 ? ComponentType.UINT16 : ComponentType.UINT32);
                indicesGltfAccessor.count = indexEndIndex - indexStartIndex;

                gltfMeshPrimitive.indices = file.accessors.indexOf(indicesGltfAccessor);

            }
        }

        return skeletonNode;
    }


    private GltfAccessor genericGltfBuffer(String name, ByteBuffer buffer, ElementType elementType, int gltfTypeId,
                                           int vertexCount, boolean normalized, boolean embedBuffers) {
        if (embedBuffers)
            return writeInternalGltfBuffer(name, getUri(buffer), buffer.capacity(), elementType, gltfTypeId, vertexCount, normalized);
        return writeExternalGltfBuffer(name, buffer, buffer.capacity(), elementType, gltfTypeId, vertexCount, normalized);
    }

    private GltfAccessor writeInternalGltfBuffer(String name, String bufferUri, int bufferCapacity, ElementType
        elementType, int gltfTypeId, int vertexCount, boolean normalized) {
        final GltfBuffer weightGltfBuffer = new GltfBuffer(file);
        weightGltfBuffer.name = name;
        weightGltfBuffer.uri = bufferUri;
        weightGltfBuffer.byteLength = bufferCapacity;

        final GltfBufferView bufferView = new GltfBufferView(file, weightGltfBuffer);
        bufferView.byteOffset = 0;
        bufferView.byteLength = bufferCapacity;

        final GltfAccessor accessor = new GltfAccessor(file, bufferView);
        accessor.type = elementType.name();
        accessor.componentType = gltfTypeId;
        accessor.count = vertexCount;
        accessor.normalized = normalized;
        return accessor;
    }

    private GltfAccessor writeExternalGltfBuffer(String name, ByteBuffer buffer, int bufferCapacity, ElementType
        elementType, int gltfTypeId, int vertexCount, boolean normalized) {
        final GltfBuffer weightGltfBuffer = new GltfBuffer(file);
        weightGltfBuffer.name = name;
        try {
            Files.write(Path.of(getBuffersPath().toString(), name + ".bin"), buffer.array());

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        weightGltfBuffer.uri = "./dbuffers/" + name + ".bin";
        weightGltfBuffer.byteLength = bufferCapacity;

        final GltfBufferView bufferView = new GltfBufferView(file, weightGltfBuffer);
        bufferView.byteOffset = 0;
        bufferView.byteLength = bufferCapacity;

        final GltfAccessor accessor = new GltfAccessor(file, bufferView);
        accessor.type = elementType.name();
        accessor.componentType = gltfTypeId;
        accessor.count = vertexCount;
        accessor.normalized = normalized;
        return accessor;
    }

    @NotNull
    private static String getUri(ByteBuffer buffer) {
        return "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(IOUtils.getBytesExact(buffer.position(0), buffer.capacity()));
    }

}
