package com.shade.decima.ui.data.viewer.model.model;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.messages.impl.IndexArrayResourceHandler.HwIndexArray;
import com.shade.decima.model.rtti.messages.impl.VertexArrayResourceHandler.HwVertexArray;
import com.shade.decima.model.rtti.messages.impl.VertexArrayResourceHandler.HwVertexStream;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.decima.ui.data.handlers.custom.PackingInfoHandler;
import com.shade.decima.ui.data.viewer.model.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.model.data.ElementType;
import com.shade.decima.ui.data.viewer.model.model.data.StorageType;
import com.shade.decima.ui.data.viewer.model.model.dmf.*;
import com.shade.decima.ui.data.viewer.model.model.utils.Transform;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.exporter.TextureExporterPNG;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.shade.decima.ui.data.viewer.texture.TextureViewer.getImageProvider;

public class DMFExporter extends ModelExporterShared implements ModelExporter {
    public static class Provider implements ModelExporterProvider {
        @NotNull
        @Override
        public ModelExporter create(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath) {
            return new DMFExporter(project, exportSettings, outputPath);
        }

        @NotNull
        @Override
        public String getExtension() {
            return "dmf";
        }

        @NotNull
        @Override
        public String getName() {
            return "DMF Scene";
        }
    }

    private static final Logger log = LoggerFactory.getLogger(DMFExporter.class);
    private static final Map<String, AccessorDescriptor> SEMANTIC_DESCRIPTORS = Map.ofEntries(
        Map.entry("Pos", new AccessorDescriptor("POSITION", ElementType.VEC3, ComponentType.FLOAT32, false, false)),
        Map.entry("TangentBFlip", new AccessorDescriptor("TANGENT", ElementType.VEC4, ComponentType.FLOAT32, false, true)),
        Map.entry("Tangent", new AccessorDescriptor("TANGENT", ElementType.VEC4, ComponentType.FLOAT32, false, true)),
        Map.entry("Normal", new AccessorDescriptor("NORMAL", ElementType.VEC3, ComponentType.FLOAT32, false, true)),
        Map.entry("Color", new AccessorDescriptor("COLOR_0", ElementType.VEC4, ComponentType.UINT8, true, true)),
        Map.entry("UV0", new AccessorDescriptor("TEXCOORD_0", ElementType.VEC2, ComponentType.FLOAT32, false, false)),
        Map.entry("UV1", new AccessorDescriptor("TEXCOORD_1", ElementType.VEC2, ComponentType.FLOAT32, false, false)),
        Map.entry("UV2", new AccessorDescriptor("TEXCOORD_2", ElementType.VEC2, ComponentType.FLOAT32, false, false)),
        Map.entry("UV3", new AccessorDescriptor("TEXCOORD_3", ElementType.VEC2, ComponentType.FLOAT32, false, false)),
        Map.entry("UV4", new AccessorDescriptor("TEXCOORD_4", ElementType.VEC2, ComponentType.FLOAT32, false, false)),
        Map.entry("UV5", new AccessorDescriptor("TEXCOORD_5", ElementType.VEC2, ComponentType.FLOAT32, false, false)),
        Map.entry("UV6", new AccessorDescriptor("TEXCOORD_6", ElementType.VEC2, ComponentType.FLOAT32, false, false)),
        Map.entry("BlendIndices", new AccessorDescriptor("JOINTS_0", ElementType.VEC4, ComponentType.UINT16, true, false)),
        Map.entry("BlendIndices2", new AccessorDescriptor("JOINTS_1", ElementType.VEC4, ComponentType.UINT16, true, false)),
        Map.entry("BlendIndices3", new AccessorDescriptor("JOINTS_2", ElementType.VEC4, ComponentType.UINT16, true, false)),
        Map.entry("BlendWeights", new AccessorDescriptor("WEIGHTS_0", ElementType.VEC4, ComponentType.FLOAT32, false, false)),
        Map.entry("BlendWeights2", new AccessorDescriptor("WEIGHTS_1", ElementType.VEC4, ComponentType.FLOAT32, false, false)),
        Map.entry("BlendWeights3", new AccessorDescriptor("WEIGHTS_2", ElementType.VEC4, ComponentType.FLOAT32, false, false))
    );
    private final RTTITypeRegistry registry;
    private final PackfileManager manager;
    private final ExportSettings exportSettings;
    private final Path outputPath;
    private final Stack<DMFCollection> collectionStack;
    private int depth = 0;
    private DMFSceneFile scene;
    private DMFSkeleton masterSkeleton = null;

    public DMFExporter(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath) {
        registry = project.getTypeRegistry();
        manager = project.getPackfileManager();
        this.exportSettings = exportSettings;
        this.outputPath = outputPath;
        collectionStack = new Stack<>();

    }


    private Path getBuffersPath() throws IOException {
        Path buffersPath = outputPath.resolve("dbuffers");
        Files.createDirectories(buffersPath);
        return buffersPath;
    }

    public DMFSceneFile export(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        scene = new DMFSceneFile(1);
        final DMFCollection rootCollection = scene.createCollection(resourceName);
        collectionStack.push(rootCollection);
        exportResource(monitor, core, object, resourceName);
        collectionStack.pop();
        return scene;
    }

    private void exportResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        log.info("Exporting {}", object.type().getTypeName());
        switch (object.type().getTypeName()) {
            case "ArtPartsDataResource" -> exportArtPartsDataResource(monitor, core, object, resourceName);
            case "ArtPartsSubModelResource" -> exportArtPartsSubModelResource(monitor, core, object, resourceName);
            case "ObjectCollection" -> exportObjectCollection(monitor, core, object, resourceName);
//            case "StaticMeshInstance" -> exportStaticMeshInstance(monitor, core, object);
//            case "Terrain" -> exportTerrainResource(monitor, core, object);
            case "LodMeshResource" -> exportLodMeshResource(monitor, core, object, resourceName);
            case "MultiMeshResource" -> exportMultiMeshResource(monitor, core, object, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" -> exportRegularSkinnedMeshResource(monitor, core, object, resourceName);
            default -> throw new IllegalArgumentException("Unsupported resource: " + object.type());
        }
    }


    private void exportArtPartsDataResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        DMFNode sceneRoot = new DMFModelGroup("SceneRoot");
        DMFNode model;
        try (ProgressMonitor.Task artPartTask = monitor.begin("Exporting ArtPartsDataResource RootModel", 2)) {
            final RTTIReference representationSkeletonRef = object.ref("RepresentationSkeleton");
            final FollowResult repSkeleton = follow(representationSkeletonRef, core);

            if (repSkeleton != null) {
                RTTIObject[] defaultPos = object.get("DefaultPoseTranslations");
                RTTIObject[] defaultRot = object.get("DefaultPoseRotations");
                DMFSkeleton skeleton = new DMFSkeleton();
                final RTTIObject[] joints = repSkeleton.object().objs("Joints");
                for (short i = 0; i < joints.length; i++) {
                    RTTIObject joint = joints[i];
                    double[] rotations;
                    if (defaultRot.length > 0) {
                        rotations = new double[]{defaultRot[i].f32("X"), defaultRot[i].f32("Y"), defaultRot[i].f32("Z"), defaultRot[i].f32("W")};
                    } else {
                        rotations = new double[]{0d, 0d, 0d, 1d};
                    }

                    final Transform boneTransform = new Transform(
                        new double[]{defaultPos[i].f32("X"), defaultPos[i].f32("Y"), defaultPos[i].f32("Z")},
                        rotations,
                        new double[]{1.d, 1.d, 1.d}
                    );
                    DMFTransform matrix = DMFTransform.fromTransform(boneTransform);
                    DMFBone bone = skeleton.newBone(joint.str("Name"), matrix, joint.i16("ParentIndex"));
                    bone.localSpace = true;
                }
                masterSkeleton = skeleton;
            }

            try (ProgressMonitor.Task task = artPartTask.split(1).begin("Exporting RootModel", 1)) {
                final FollowResult rootModelRes = Objects.requireNonNull(follow(object.ref("RootModel"), core));
                model = toModel(task.split(1), rootModelRes.binary(), rootModelRes.object(), nameFromReference(object.ref("RootModel"), resourceName));
            }
            RTTIReference[] subModels = object.get("SubModelPartResources");
            try (ProgressMonitor.Task task = artPartTask.split(1).begin("Exporting SubModelPartResources", subModels.length)) {
                for (int i = 0; i < subModels.length; i++) {
                    RTTIReference subPart = subModels[i];
                    FollowResult subPartRes = Objects.requireNonNull(follow(subPart, core));
                    DMFNode node = toModel(task.split(1), subPartRes.binary(), subPartRes.object(), "SubModel%d_%s".formatted(i, nameFromReference(subPart, resourceName)));
                    model.children.add(node);
                }
            }
        }
        sceneRoot.children.add(model);
        scene.models.add(sceneRoot);
    }

    private void exportLodMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        DMFNode node = toModel(monitor, core, object, resourceName);
        scene.models.add(node);
    }

    private void exportArtPartsSubModelResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        DMFNode node = toModel(monitor, core, object, resourceName);
        scene.models.add(node);
    }

    private void exportMultiMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object, @NotNull String resourceName
    ) throws IOException {
        DMFModelGroup group = new DMFModelGroup("SceneRoot");
        scene.models.add(group);
        DMFNode node = toModel(monitor, core, object, resourceName);
        group.children.add(node);
    }

    private void exportObjectCollection(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        DMFModelGroup group = new DMFModelGroup(resourceName);
        scene.models.add(group);
        int itemId = 0;
        RTTIReference[] objects = object.get("Objects");
        try (ProgressMonitor.Task task = monitor.begin("Exporting ObjectCollection Objects", objects.length)) {
            for (RTTIReference rttiReference : objects) {
                FollowResult refObject = Objects.requireNonNull(follow(rttiReference, core));
                DMFNode node = toModel(task.split(1), refObject.binary(), refObject.object(), nameFromReference(rttiReference, "%s_Object_%d".formatted(resourceName, itemId)));
                group.children.add(node);
                itemId++;
            }
        }
    }


    private DMFNode toModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        depth += 1;
        log.info("{}Converting {}", "\t".repeat(depth), object.type().getTypeName());
        var res = switch (object.type().getTypeName()) {
            case "PrefabResource" -> prefabResourceToModel(monitor, core, object, resourceName);
            case "ModelPartResource" -> modelPartResourceToModel(monitor, core, object, resourceName);
            case "ArtPartsSubModelWithChildrenResource" -> artPartsSubModelWithChildrenResourceToModel(monitor, core, object, resourceName);
            case "ArtPartsSubModelResource" -> artPartsSubModelResourceToModel(monitor, core, object, resourceName);
            case "PrefabInstance" -> prefabInstanceToModel(monitor, core, object, resourceName);
            case "ObjectCollection" -> objectCollectionToModel(monitor, core, object, resourceName);
            case "StaticMeshInstance" -> staticMeshInstanceToModel(monitor, core, object, resourceName);
//            case "Terrain" -> terrainResourceToModel(monitor,core, object);
            case "LodMeshResource" -> lodMeshResourceToModel(monitor, core, object, resourceName);
            case "MultiMeshResource" -> multiMeshResourceToModel(monitor, core, object, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" -> regularSkinnedMeshResourceToModel(monitor, core, object, resourceName);
            default -> {
                log.info("{}Cannot export {}", "\t".repeat(depth), object.type().getTypeName());
                yield null;
            }
        };
        depth -= 1;
        return res;
    }

    private DMFNode artPartsSubModelResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference meshResourceRef = object.ref("MeshResource");
        DMFNode model;
        DMFCollection subModelResourceCollection = scene.createCollection(resourceName, collectionStack.peek(), !object.bool("IsHideDefault"));
        collectionStack.push(subModelResourceCollection);

        RTTIReference extraMeshResourceRef = object.ref("ExtraResource");
        FollowResult extraResourceRef = follow(extraMeshResourceRef, core);

        if (extraResourceRef != null && (extraResourceRef.object().type().getTypeName().equals("ArtPartsCoverModelResource") ||
            extraResourceRef.object().type().getTypeName().equals("ArtPartsCoverAndAnimResource"))) {
            final FollowResult repSkeleton = Objects.requireNonNull(follow(object.ref("Skeleton"), core));
            RTTIObject[] defaultPos = extraResourceRef.object().get("DefaultPoseTranslations");
            RTTIObject[] defaultRot = extraResourceRef.object().get("DefaultPoseRotations");
            DMFSkeleton skeleton = new DMFSkeleton();
            final RTTIObject[] joints = repSkeleton.object().objs("Joints");
            for (short i = 0; i < joints.length; i++) {
                RTTIObject joint = joints[i];
                final Transform boneTransform = new Transform(
                    new double[]{defaultPos[i].f32("X"), defaultPos[i].f32("Y"), defaultPos[i].f32("Z")},
                    new double[]{defaultRot[i].f32("X"), defaultRot[i].f32("Y"), defaultRot[i].f32("Z"), defaultRot[i].f32("W")},
                    new double[]{1.d, 1.d, 1.d}
                );
                DMFTransform matrix = DMFTransform.fromTransform(boneTransform);
                final short parentIndex = joint.i16("ParentIndex");
                DMFBone bone;
                if (parentIndex == -1) {
                    bone = skeleton.newBone(joint.str("Name"), matrix);
                } else {
                    bone = skeleton.newBone(joint.str("Name"), matrix, skeleton.findBoneId(joints[parentIndex].str("Name")));
                }
                bone.localSpace = true;
            }
            masterSkeleton = skeleton;
        }

        FollowResult meshResourceRes = follow(meshResourceRef, core);
        if (meshResourceRes != null) {
            try (ProgressMonitor.Task task = monitor.begin("Exporting ArtPartsSubModelResource MeshResource", 1)) {
                model = toModel(task.split(1), meshResourceRes.binary(), meshResourceRes.object(), nameFromReference(meshResourceRef, resourceName));
            }
        } else {
            model = new DMFModelGroup(resourceName);
        }

        FollowResult extraMeshResourceRes = follow(extraMeshResourceRef, core);
        if (extraMeshResourceRes != null) {
            DMFNode extraModel;
            try (ProgressMonitor.Task task = monitor.begin("Exporting ArtPartsSubModelResource ExtraResource", 1)) {
                extraModel = toModel(task.split(1), extraMeshResourceRes.binary(), extraMeshResourceRes.object(), "EXTRA_" + nameFromReference(extraMeshResourceRef, resourceName));
            }
            if (extraModel != null) {
                model.children.add(extraModel);
            }
        }
        model.addToCollection(subModelResourceCollection, scene);
        collectionStack.pop();
        return model;
    }

    private DMFNode artPartsSubModelWithChildrenResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference meshResourceRef = object.ref("ArtPartsSubModelPartResource");
        DMFNode model;
        DMFCollection subModelPartsCollection = scene.createCollection(resourceName, collectionStack.peek(), !object.bool("IsHideDefault"));
        collectionStack.push(subModelPartsCollection);
        FollowResult meshResourceRes = follow(meshResourceRef, core);
        if (meshResourceRes != null) {
            try (ProgressMonitor.Task artPartTask = monitor.begin("Exporting ArtPartsSubModelWithChildrenResource", 1)) {
                model = toModel(artPartTask.split(1), meshResourceRes.binary(), meshResourceRes.object(), nameFromReference(meshResourceRef, resourceName));
            }
        } else {
            model = new DMFModelGroup(resourceName);
        }
        RTTIReference[] children = object.get("Children");
        if (children.length > 0) {
            try (ProgressMonitor.Task task = monitor.begin("Exporting Children", children.length)) {
                for (int i = 0; i < children.length; i++) {
                    RTTIReference subPart = children[i];
                    FollowResult subPartRes = Objects.requireNonNull(follow(subPart, core));
                    model.children.add(toModel(task.split(1), subPartRes.binary(), subPartRes.object(), nameFromReference(subPart, "child%d_%s".formatted(i, resourceName))));
                }
            }
        }

        model.addToCollection(subModelPartsCollection, scene);
        collectionStack.pop();
        return model;
    }

    private DMFNode modelPartResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference meshResourceRef = object.ref("MeshResource");
        FollowResult meshResource = Objects.requireNonNull(follow(meshResourceRef, core));
        try (ProgressMonitor.Task task = monitor.begin("Exporting ModelPartResource MeshResource", 1)) {
            return toModel(task.split(1), meshResource.binary(), meshResource.object(), nameFromReference(meshResourceRef, resourceName));
        }
    }

    private DMFNode prefabResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference objectCollection = object.ref("ObjectCollection");
        FollowResult prefabResource = Objects.requireNonNull(follow(objectCollection, core));
        try (ProgressMonitor.Task task = monitor.begin("Exporting PrefabResource ObjectCollection", 1)) {
            return toModel(task.split(1), prefabResource.binary(), prefabResource.object(), nameFromReference(objectCollection, resourceName));
        }
    }

    private DMFNode prefabInstanceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference prefab = object.ref("Prefab");
        FollowResult prefabResource = Objects.requireNonNull(follow(prefab, core));
        DMFNode node;
        try (ProgressMonitor.Task task = monitor.begin("Exporting PrefabInstance Prefab", 1)) {
            node = toModel(task.split(1), prefabResource.binary(), prefabResource.object(), nameFromReference(prefab, resourceName));
        }
        if (node == null) {
            return null;
        }
        if (node.transform != null) {
            throw new IllegalStateException("Unexpected transform");
        }
        Transform transform = worldTransformToTransform(object.get("Orientation"));
        node.transform = DMFTransform.fromTransform(transform);
        return node;
    }

    private DMFNode staticMeshInstanceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference resource = object.ref("Resource");
        FollowResult meshResource = Objects.requireNonNull(follow(resource, core));
        try (ProgressMonitor.Task task = monitor.begin("Exporting StaticMeshInstance Resource", 1)) {
            return toModel(task.split(1), meshResource.binary(), meshResource.object(), nameFromReference(resource, resourceName));
        }
    }

    private DMFNode objectCollectionToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference[] objects = object.get("Objects");
        DMFModelGroup group = new DMFModelGroup("Collection %s".formatted(resourceName));
        int itemId = 0;
        try (ProgressMonitor.Task task = monitor.begin("Exporting ObjectCollection Objects", objects.length)) {
            for (RTTIReference rttiReference : objects) {
                FollowResult refObject = Objects.requireNonNull(follow(rttiReference, core));
                DMFNode node = toModel(task.split(1), refObject.binary(), refObject.object(), "%s_Object_%d".formatted(nameFromReference(rttiReference, resourceName), itemId));
                itemId++;
                if (node == null) {
                    continue;
                }
                group.children.add(node);
            }
        }
        return group;
    }

    private DMFNode lodMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIObject[] meshes = object.get("Meshes");
        if (meshes.length == 0) {
            return null;
        }
        DMFLodModel lodModel = new DMFLodModel();
        try (ProgressMonitor.Task task = monitor.begin("Exporting lods", meshes.length)) {
            for (int lodId = 0; lodId < meshes.length; lodId++) {
                RTTIObject lodRef = meshes[lodId];
                RTTIReference meshRef = lodRef.ref("Mesh");
                final var mesh = Objects.requireNonNull(follow(meshRef, core));
                final DMFNode lod = toModel(task.split(1), mesh.binary(), mesh.object(), "%s_LOD%d".formatted(nameFromReference(meshRef, resourceName), 0));
                lodModel.addLod(lod, lodId, lodRef.f32("Distance"));
            }
        }
        return lodModel;
    }

    private DMFNode multiMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        DMFModelGroup group = new DMFModelGroup();
        RTTIObject[] parts = object.get("Parts");
        try (ProgressMonitor.Task task = monitor.begin("Exporting MultiMeshResource Parts", parts.length)) {
            for (int partId = 0; partId < parts.length; partId++) {
                RTTIObject part = parts[partId];
                RTTIReference meshRef = part.ref("Mesh");
                final var mesh = Objects.requireNonNull(follow(meshRef, core));
                Transform transform = worldTransformToTransform(part.obj("Transform"));
                DMFNode model = toModel(task.split(1), mesh.binary(), mesh.object(), "%s_Part%d".formatted(nameFromReference(meshRef, resourceName), partId));
                if (model == null) continue;
                if (model.transform != null) {
                    throw new IllegalStateException("Model already had transforms, please handle me!");
                }
                model.transform = DMFTransform.fromTransform(transform);
                group.children.add(model);
            }
        }
        return group;
    }

    private void exportRegularSkinnedMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        DMFNode sceneRoot = new DMFModelGroup("SceneRoot");
        scene.models.add(sceneRoot);

        DMFModel model;
        model = regularSkinnedMeshResourceToModel(monitor, core, object, resourceName);
        if (model != null) {
            sceneRoot.children.add(model);
        }
    }

    private DMFModel regularSkinnedMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        DrawFlags flags = DrawFlags.fromDataAndRegistry(object.obj("DrawFlags").i32("Data"), registry);
        if (!flags.renderType().equals("Normal")) {
            return null;
        }

        DMFModel model = new DMFModel(resourceName);
        DMFMesh mesh = new DMFMesh();
        if (object.type().getTypeName().equals("RegularSkinnedMeshResource")) {
            DMFSkeleton skeleton = new DMFSkeleton();
            final RTTIObject skeletonObj = Objects.requireNonNull(follow(object.ref("Skeleton"), core)).object();
            final RTTIObject meshJointBindings = Objects.requireNonNull(follow(object.ref("SkinnedMeshJointBindings"), core)).object();
            final RTTIObject[] joints = skeletonObj.get("Joints");
            final short[] jointIndexList = meshJointBindings.get("JointIndexList");
            final RTTIObject[] inverseBindMatrices = meshJointBindings.get("InverseBindMatrices");

            for (short i = 0; i < joints.length; i++) {
                int localBoneId = IOUtils.indexOf(jointIndexList, i);
                if (localBoneId == -1) {
                    if (masterSkeleton != null) {
                        DMFBone masterBone = masterSkeleton.findBone(joints[i].str("Name"));
                        DMFBone bone;
                        if (masterBone == null)
                            continue;
                        if (masterBone.parentId != -1)
                            bone = skeleton.newBone(masterBone.name, masterBone.transform, skeleton.findBoneId(masterSkeleton.bones.get(masterBone.parentId).name));
                        else
                            bone = skeleton.newBone(masterBone.name, masterBone.transform);
                        bone.localSpace = true;
                    }
                    continue;
                }
                RTTIObject joint = joints[i];
                DMFTransform matrix;
                boolean localSpace = false;

                if (masterSkeleton != null) {
                    DMFBone masterBone = masterSkeleton.findBone(joints[i].str("Name"));
                    if (masterBone == null) {
                        matrix = DMFTransform.fromMatrix(InvertedMatrix4x4TransformToMatrix(inverseBindMatrices[localBoneId]).inverted());
                    } else {
                        matrix = masterBone.transform;
                        localSpace = true;
                    }
                } else {
                    matrix = DMFTransform.fromMatrix(InvertedMatrix4x4TransformToMatrix(inverseBindMatrices[localBoneId]).inverted());
                }

                final short parentIndex = joint.i16("ParentIndex");
                DMFBone bone;
                if (parentIndex == -1) {
                    bone = skeleton.newBone(joint.str("Name"), matrix);
                } else {
                    final String parentName = joints[parentIndex].str("Name");
                    bone = skeleton.newBone(joint.str("Name"), matrix, skeleton.findBoneId(parentName));
                }
                bone.localSpace = localSpace;
            }

            for (short targetId : jointIndexList) {
                RTTIObject targetBone = joints[targetId];
                mesh.boneRemapTable.put(targetId, (short) skeleton.findBoneId(targetBone.str("Name")));
            }

            model.setSkeleton(skeleton, scene);
        }
        final String dataSourceObj = object.obj("DataSource").str("Location");
        final String dataSourceLocation = "%s.core.stream".formatted(dataSourceObj);
        final Packfile dataSourcePackfile = Objects.requireNonNull(manager.findAny(dataSourceLocation), "Can't find referenced data source");
        final ByteBuffer dataSource = ByteBuffer
            .wrap(dataSourcePackfile.extract(dataSourceLocation))
            .order(ByteOrder.LITTLE_ENDIAN);
        DMFBuffer buffer;

        buffer = new DMFInternalBuffer(dataSource);

        buffer.originalName = dataSourceLocation;
        Map<RTTIObject, Map.Entry<Integer, Integer>> bufferOffsets = new HashMap<>();

        RTTIReference[] primitivesRefs = object.get("Primitives");
        RTTIReference[] shadingGroupsRefs = object.get("ShadingGroups");
        if (primitivesRefs.length != shadingGroupsRefs.length) {
            throw new IllegalStateException("Primitives count does not match ShadingGroups count!");
        }
        int dataSourceOffset = 0;
        for (RTTIReference primitivesRef : primitivesRefs) {
            final var primitiveRes = Objects.requireNonNull(follow(primitivesRef, core));
            final RTTIObject primitiveObj = primitiveRes.object();
            RTTIObject vertexArrayObj = Objects.requireNonNull(follow(primitiveObj.ref("VertexArray"), primitiveRes.binary())).object();
            RTTIObject indexArrayObj = Objects.requireNonNull(follow(primitiveObj.ref("IndexArray"), primitiveRes.binary())).object();
            final HwVertexArray vertices = vertexArrayObj.obj("Data").cast();
            final HwIndexArray indices = indexArrayObj.obj("Data").cast();

            final int vertexCount = vertices.vertexCount;
            final int indexCount = indices.indexCount;

            RTTIObject vertexArrayUUID = vertexArrayObj.get("ObjectUUID");
            if (!bufferOffsets.containsKey(vertexArrayUUID)) {
                bufferOffsets.put(vertexArrayUUID, Map.entry(dataSourceOffset, bufferOffsets.size()));
                for (RTTIObject stream : vertices.streams) {
                    final int stride = stream.i32("Stride");
                    dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
                }
            }
            RTTIObject indicesArrayUUID = indexArrayObj.get("ObjectUUID");
            if (!bufferOffsets.containsKey(indicesArrayUUID)) {
                bufferOffsets.put(indicesArrayUUID, Map.entry(dataSourceOffset, bufferOffsets.size()));

                dataSourceOffset += IOUtils.alignUp(indices.getIndexSize() * indexCount, 256);
            }

        }
        try (ProgressMonitor.Task exportTask = monitor.begin("Exporting primitives", primitivesRefs.length)) {
            for (int i = 0; i < primitivesRefs.length; i++) {
                RTTIReference primitivesRef = primitivesRefs[i];
                RTTIReference shadingGroupRef = shadingGroupsRefs[i];
                final var primitiveRes = Objects.requireNonNull(follow(primitivesRef, core));
                RTTIObject primitiveObj = primitiveRes.object();
                RTTIObject shadingGroupObj = Objects.requireNonNull(follow(shadingGroupRef, core)).object();
                RTTIObject vertexArrayObj = Objects.requireNonNull(follow(primitiveObj.ref("VertexArray"), primitiveRes.binary())).object();
                RTTIObject indexArrayObj = Objects.requireNonNull(follow(primitiveObj.ref("IndexArray"), primitiveRes.binary())).object();
                RTTIObject vertexArrayUUID = vertexArrayObj.get("ObjectUUID");
                RTTIObject indicesArrayUUID = indexArrayObj.get("ObjectUUID");
                final HwVertexArray vertices = vertexArrayObj.obj("Data").cast();
                final HwIndexArray indices = indexArrayObj.obj("Data").cast();


                Map.Entry<Integer, Integer> offsetAndGroupId = bufferOffsets.get(vertexArrayUUID);
                dataSourceOffset = offsetAndGroupId.getKey();


                final DMFPrimitive primitive = mesh.newPrimitive(vertices.vertexCount, DMFVertexBufferType.SINGLEBUFFER, 0, vertices.vertexCount,
                    indices.getIndexSize(), indices.indexCount, primitiveObj.i32("StartIndex"), primitiveObj.i32("EndIndex"));
                for (RTTIObject streamObj : vertices.streams) {
                    HwVertexStream stream = streamObj.cast();
                    final int stride = stream.stride;
                    DMFBufferView bufferView = new DMFBufferView();

                    bufferView.offset = dataSourceOffset;
                    bufferView.size = stride * vertices.vertexCount;
                    bufferView.setBuffer(buffer, scene);
                    RTTIObject[] elements = stream.elements;
                    for (int j = 0; j < elements.length; j++) {
                        RTTIObject element = elements[j];
                        final int offset = element.i8("Offset");
                        int realElementSize = 0;
                        if (j < elements.length - 1) {
                            realElementSize = elements[j + 1].i8("Offset") - offset;
                        } else if (j == 0) {
                            realElementSize = stride;
                        } else if (j == elements.length - 1) {
                            realElementSize = stride - offset;
                        }
                        String elementType = element.str("Type");
                        final AccessorDescriptor descriptor = SEMANTIC_DESCRIPTORS.get(elementType);
                        DMFVertexAttribute attribute = new DMFVertexAttribute();
                        StorageType storageType = StorageType.fromString(element.str("StorageType"));
                        attribute.offset = offset;
                        attribute.semantic = descriptor.semantic();
                        attribute.size = realElementSize;
                        attribute.elementType = storageType.getTypeName();
                        attribute.elementCount = realElementSize / storageType.getSize();
                        attribute.stride = stride;
                        attribute.setBufferView(bufferView, scene);
                        primitive.vertexAttributes.put(descriptor.semantic(), attribute);
                    }
                    dataSourceOffset += IOUtils.alignUp(stride * vertices.vertexCount, 256);
                }

                DMFBufferView bufferView = new DMFBufferView();
                offsetAndGroupId = bufferOffsets.get(indicesArrayUUID);
                primitive.groupingId = offsetAndGroupId.getValue();

                bufferView.offset = offsetAndGroupId.getKey();
                bufferView.size = indices.getIndexSize() * indices.indexCount;
                bufferView.setBuffer(buffer, scene);
                primitive.setIndexBufferView(bufferView, scene);

                RTTIObject materialUUID = shadingGroupObj.get("ObjectUUID");
                String materialName = uuidToString(materialUUID);
                DMFMaterial material;
                if (scene.getMaterial(materialName) == null) {
                    material = scene.createMaterial(materialName);
                    exportMaterial(exportTask.split(1), shadingGroupObj, material, core);
                } else {
                    material = scene.getMaterial(materialName);
                    exportTask.worked(1);
                }
                primitive.setMaterial(material, scene);

            }
        }

        model.mesh = mesh;
        model.addToCollection(collectionStack.peek(), scene);
        return model;
    }

    private void exportMaterial(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTIObject shadingGroup,
        @NotNull DMFMaterial material,
        @NotNull CoreBinary binary
    ) throws IOException {
        RTTITypeEnum textureSetTypeEnum = registry.find("ETextureSetType");
        RTTIReference renderEffectRef = shadingGroup.ref("RenderEffect");
        FollowResult renderEffectRes = follow(renderEffectRef, binary);
        if (renderEffectRes == null) {
            return;
        }
        RTTIObject renderEffect = renderEffectRes.object();
        material.type = renderEffect.str("EffectType");
        if (!exportSettings.exportTextures) {
            return;
        }
        for (RTTIObject techniqueSet : renderEffect.objs("TechniqueSets")) {
            for (RTTIObject renderTechnique : techniqueSet.objs("RenderTechniques")) {
                final String techniqueType = renderTechnique.str("TechniqueType");
                if (!(techniqueType.equals("Deferred") || techniqueType.equals("CustomDeferred") || techniqueType.equals("DeferredEmissive"))) {
                    log.warn("Skipped %s".formatted(techniqueType));
                    continue;
                }

                final RTTIObject[] textureBindings = renderTechnique.get("TextureBindings");
                for (RTTIObject textureBinding : textureBindings) {
                    RTTIReference textureRef = textureBinding.ref("TextureResource");
                    FollowResult textureRes = follow(textureRef, binary);
                    if (textureRes == null) {
                        continue;
                    }
                    int packedData = textureBinding.i32("PackedData");
                    int usageType = packedData >> 2 & 15;
                    String textureUsageName = textureSetTypeEnum.valueOf(usageType).name();
                    if (textureUsageName.equals("Invalid")) {
                        textureUsageName = nameFromReference(textureRef, "Texture_%s".formatted(uuidToString(textureRef)));
                    }

                    RTTIObject textureObj = textureRes.object();
                    if (textureObj.type().getTypeName().equals("Texture")) {
                        String textureName = nameFromReference(textureRef, uuidToString(textureObj.obj("ObjectUUID")));
                        log.debug("Extracting \"{}\" texture", textureName);

                        if (scene.getTexture(textureName) != null) {
                            int textureId2 = scene.textures.indexOf(scene.getTexture(textureName));
                            if (!material.textureIds.containsValue(textureId2)) {
                                material.textureIds.put(textureUsageName, textureId2);
                            }
                            continue;

                        }
                        DMFTexture dmfTexture = exportTexture(textureObj, textureName);
                        if (dmfTexture == null) {
                            dmfTexture = DMFTexture.nonExportableTexture(textureName);
                        }
                        dmfTexture.usageType = textureUsageName;
                        material.textureIds.put(textureUsageName, scene.textures.indexOf(dmfTexture));

                    } else if (textureObj.type().getTypeName().equals("TextureSet")) {
                        RTTIObject[] entries = textureObj.get("Entries");

                        DMFTextureDescriptor descriptor = new DMFTextureDescriptor();
                        descriptor.usageType = textureUsageName;

                        for (int i = 0; i < entries.length; i++) {
                            RTTIObject entry = entries[i];
                            int usageInfo = entry.i32("PackingInfo");
                            String tmp = PackingInfoHandler.getInfo(usageInfo & 0xFF) +
                                PackingInfoHandler.getInfo(usageInfo >>> 8 & 0xff) +
                                PackingInfoHandler.getInfo(usageInfo >>> 16 & 0xff) +
                                PackingInfoHandler.getInfo(usageInfo >>> 24 & 0xff);
                            if (tmp.contains(textureUsageName)) {
                                RTTIReference textureSetTextureRef = entry.ref("Texture");
                                final FollowResult textureSetResult = follow(textureSetTextureRef, textureRes.binary());
                                if (textureSetResult == null) {
                                    continue;
                                }
                                final String textureName = nameFromReference(textureRef, "Texture_%s".formatted(uuidToString(textureSetTextureRef))) + "_%d".formatted(i);
                                DMFTexture texture = exportTexture(textureSetResult.object(), textureName);
                                descriptor.textureId = scene.textures.indexOf(texture);
                                if (PackingInfoHandler.getInfo(usageInfo & 0xFF).contains(textureUsageName)) {
                                    descriptor.channels += "R";
                                }
                                if (PackingInfoHandler.getInfo(usageInfo >>> 8 & 0xff).contains(textureUsageName)) {
                                    descriptor.channels += "G";
                                }
                                if (PackingInfoHandler.getInfo(usageInfo >>> 16 & 0xff).contains(textureUsageName)) {
                                    descriptor.channels += "B";
                                }
                                if (PackingInfoHandler.getInfo(usageInfo >>> 24 & 0xff).contains(textureUsageName)) {
                                    descriptor.channels += "A";
                                }
                                break;
                            }
                        }
                        material.textureDescriptors.add(descriptor);
                    } else {
                        log.warn("Texture of type {} not supported", textureObj.type().getTypeName());
                    }
                }
            }
        }
    }

    private DMFTexture exportTexture(@NotNull RTTIObject texture, @NotNull String textureName) throws IOException {
        for (DMFTexture dmfTexture : scene.textures) {
            if (dmfTexture.name.equals(textureName)) {
                return dmfTexture;
            }
        }
        switch (texture.type().getTypeName()) {
            case "Texture":
                break;
            case "TextureList":
                texture = texture.objs("Textures")[0];
                break;
            default:
                throw new IllegalStateException("Unsupported %s".formatted(texture.type().getTypeName()));

        }
        final ImageProvider imageProvider = getImageProvider(texture, manager);
        if (imageProvider == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new TextureExporterPNG().export(imageProvider, Set.of(), Channels.newChannel(stream));
        byte[] src = stream.toByteArray();
        DMFTexture dmfTexture;
        if (exportSettings.embedTextures) {
            DMFInternalTexture dmfInternalTexture = new DMFInternalTexture(textureName);
            dmfInternalTexture.bufferData = Base64.getEncoder().encodeToString(src);
            dmfInternalTexture.bufferSize = src.length;
            dmfTexture = dmfInternalTexture;
        } else {
            DMFExternalTexture dmfExternalTexture = new DMFExternalTexture(textureName);
            dmfExternalTexture.bufferSize = src.length;
            dmfExternalTexture.bufferFileName = textureName + ".png";
            Files.write(getBuffersPath().resolve(textureName + ".png"), src);
            dmfTexture = dmfExternalTexture;
        }
        dmfTexture.dataType = DMFDataType.PNG;

        scene.textures.add(dmfTexture);
        return dmfTexture;
    }

    @Nullable
    private FollowResult follow(@NotNull RTTIReference reference, @NotNull CoreBinary current) throws IOException {
        final CoreBinary binary;
        final RTTIObject uuid;

        if (reference instanceof RTTIReference.External ref) {
            final List<Packfile> packfiles = manager.findAll(ref.path());
            if (packfiles.isEmpty()) {
                throw new IOException("Couldn't find referenced file: " + ref.path());
            }
            if (packfiles.size() > 1) {
                throw new IOException("Multiple packfiles contain files under the same path: " + ref.path());
            }
            binary = CoreBinary.from(packfiles.get(0).extract(ref.path()), registry);
            uuid = ref.uuid();
        } else if (reference instanceof RTTIReference.Internal ref) {
            binary = current;
            uuid = ref.uuid();
        } else {
            return null;
        }

        final RTTIObject object = binary.find(uuid);

        if (object == null) {
            throw new IOException("Couldn't find referenced entry: " + GGUUIDValueHandler.toString(uuid));
        }

        return new FollowResult(binary, object);
    }

    record FollowResult(@NotNull CoreBinary binary, @NotNull RTTIObject object) {}
}
