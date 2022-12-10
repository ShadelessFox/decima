package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.decima.ui.data.handlers.custom.PackingInfoHandler;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.decima.ui.data.viewer.model.data.StorageType;
import com.shade.decima.ui.data.viewer.model.dmf.*;
import com.shade.decima.ui.data.viewer.model.utils.MathUtils;
import com.shade.decima.ui.data.viewer.model.utils.Matrix4x4;
import com.shade.decima.ui.data.viewer.model.utils.Transform;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.exporter.TextureExporterPNG;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
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

public class DMFExporter implements ModelExporter {
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
        Map.entry("Pos", new AccessorDescriptor("POSITION", ElementType.VEC3, ComponentType.FLOAT, false, false)),
        Map.entry("TangentBFlip", new AccessorDescriptor("TANGENT", ElementType.VEC4, ComponentType.FLOAT, false, true)),
        Map.entry("Tangent", new AccessorDescriptor("TANGENT", ElementType.VEC4, ComponentType.FLOAT, false, true)),
        Map.entry("Normal", new AccessorDescriptor("NORMAL", ElementType.VEC3, ComponentType.FLOAT, false, true)),
        Map.entry("Color", new AccessorDescriptor("COLOR_0", ElementType.VEC4, ComponentType.BYTE, true, true)),
        Map.entry("UV0", new AccessorDescriptor("TEXCOORD_0", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV1", new AccessorDescriptor("TEXCOORD_1", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV2", new AccessorDescriptor("TEXCOORD_2", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV3", new AccessorDescriptor("TEXCOORD_3", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV4", new AccessorDescriptor("TEXCOORD_4", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV5", new AccessorDescriptor("TEXCOORD_5", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV6", new AccessorDescriptor("TEXCOORD_6", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("BlendIndices", new AccessorDescriptor("JOINTS_0", ElementType.VEC4, ComponentType.UNSIGNED_SHORT, true, false)),
        Map.entry("BlendIndices2", new AccessorDescriptor("JOINTS_1", ElementType.VEC4, ComponentType.UNSIGNED_SHORT, true, false)),
        Map.entry("BlendIndices3", new AccessorDescriptor("JOINTS_2", ElementType.VEC4, ComponentType.UNSIGNED_SHORT, true, false)),
        Map.entry("BlendWeights", new AccessorDescriptor("WEIGHTS_0", ElementType.VEC4, ComponentType.FLOAT, false, false)),
        Map.entry("BlendWeights2", new AccessorDescriptor("WEIGHTS_1", ElementType.VEC4, ComponentType.FLOAT, false, false)),
        Map.entry("BlendWeights3", new AccessorDescriptor("WEIGHTS_2", ElementType.VEC4, ComponentType.FLOAT, false, false))
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

    private static String nameFromReference(@NotNull RTTIReference ref, @NotNull String resourceName) {
        if (ref.type() == RTTIReference.Type.EXTERNAL_LINK) {
            String path = ref.path();
            if (path == null)
                return resourceName;
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return resourceName;
    }

    @NotNull
    private static Transform worldTransformToMatrix(RTTIObject transformObj) {
        final var posObj = transformObj.obj("Position");
        final var oriObj = transformObj.obj("Orientation");
        final RTTIObject col0Obj = oriObj.obj("Col0");
        final RTTIObject col1Obj = oriObj.obj("Col1");
        final RTTIObject col2Obj = oriObj.obj("Col2");
        final double[] col0 = {col0Obj.f32("X"), col0Obj.f32("Y"), col0Obj.f32("Z")};
        final double[] col1 = {col1Obj.f32("X"), col1Obj.f32("Y"), col1Obj.f32("Z")};
        final double[] col2 = {col2Obj.f32("X"), col2Obj.f32("Y"), col2Obj.f32("Z")};
        double[] pos = {posObj.f64("X"), posObj.f64("Y"), posObj.f64("Z")};

        Transform transform = Transform.fromRotationAndScaleMatrix(new double[][]{col0, col1, col2});
        transform.setTranslation(pos);
        return transform;
    }

    @NotNull
    private static Matrix4x4 InvertedMatrix4x4TransformToMatrix(RTTIObject transformObj) {
        final RTTIObject col0Obj = transformObj.obj("Col0");
        final RTTIObject col1Obj = transformObj.obj("Col1");
        final RTTIObject col2Obj = transformObj.obj("Col2");
        final RTTIObject col3Obj = transformObj.obj("Col3");
        final double[] col0 = {col0Obj.f32("X"), col0Obj.f32("Y"), col0Obj.f32("Z"), col0Obj.f32("W")};
        final double[] col1 = {col1Obj.f32("X"), col1Obj.f32("Y"), col1Obj.f32("Z"), col1Obj.f32("W")};
        final double[] col2 = {col2Obj.f32("X"), col2Obj.f32("Y"), col2Obj.f32("Z"), col2Obj.f32("W")};
        final double[] col3 = {col3Obj.f32("X"), col3Obj.f32("Y"), col3Obj.f32("Z"), col3Obj.f32("W")};
        return new Matrix4x4(new double[][]{col0, col1, col2, col3}).transposed().inverted();
    }

    private static String uuidToString(RTTIObject uuid) {
        return GGUUIDValueHandler.INSTANCE.getString(uuid.type(), uuid);
    }

    private Path getBuffersPath() throws IOException {
        Path buffersPath = outputPath.resolve("dbuffers");
        Files.createDirectories(buffersPath);
        return buffersPath;
    }

    public DMFSceneFile export(@NotNull ProgressMonitor monitor,
                               @NotNull CoreBinary core,
                               @NotNull RTTIObject object,
                               @NotNull String resourceName) throws IOException {
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
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                exportRegularSkinnedMeshResource(monitor, core, object, resourceName);
            default -> throw new IllegalArgumentException("Unsupported resource: " + object.type());
        }
    }

    private DMFNode toModel(
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
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

    private void exportArtPartsDataResource(
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,


        @NotNull String resourceName
    ) throws IOException {
        Transform transform = Transform.fromRotation(0, -90, 0);
        DMFNode sceneRoot = new DMFModelGroup("SceneRoot");
        sceneRoot.transform = DMFTransform.fromTransform(transform);
        DMFNode model;
        try (ProgressMonitor.Task artPartTask = monitor.begin("Exporting ArtPartsDataResource RootModel", 2)) {
            RTTIObject repSkeleton = object.ref("RepresentationSkeleton").follow(core, manager, registry).object();
            RTTIObject[] defaultPos = object.get("DefaultPoseTranslations");
            RTTIObject[] defaultRot = object.get("DefaultPoseRotations");
            DMFSkeleton skeleton = new DMFSkeleton();
            final RTTIObject[] joints = repSkeleton.get("Joints");
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


            try (ProgressMonitor.Task task = artPartTask.split(1).begin("Exporting RootModel", 1)) {
                RTTIReference.FollowResult rootModelRes = object.ref("RootModel").follow(core, manager, registry);
                model = toModel(task.split(1), rootModelRes.binary(), rootModelRes.object(), nameFromReference(object.ref("RootModel"), resourceName));
            }
            RTTIReference[] subModels = object.get("SubModelPartResources");
            try (ProgressMonitor.Task task = artPartTask.split(1).begin("Exporting SubModelPartResources", subModels.length)) {
                for (int i = 0; i < subModels.length; i++) {
                    RTTIReference subPart = subModels[i];
                    RTTIReference.FollowResult subPartRes = subPart.follow(core, manager, registry);
                    DMFNode node = toModel(task.split(1), subPartRes.binary(), subPartRes.object(), "SubModel%d_%s".formatted(i, nameFromReference(subPart, resourceName)));
                    model.children.add(node);
                }
            }
        }
        sceneRoot.children.add(model);
        scene.models.add(sceneRoot);
    }

    private void exportLodMeshResource(
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,


        @NotNull String resourceName
    ) throws IOException {
        DMFNode node = toModel(monitor, core, object, resourceName);
        node.transform = DMFTransform.fromTransform(Transform.fromRotation(0, -90, 0));
        scene.models.add(node);
    }

    private void exportArtPartsSubModelResource(
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,

        @NotNull String resourceName
    ) throws IOException {
        DMFNode node = toModel(monitor, core, object, resourceName);
        scene.models.add(node);
    }

    private void exportMultiMeshResource(
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,


        @NotNull String resourceName
    ) throws IOException {
        DMFModelGroup group = new DMFModelGroup();
        group.name = "SceneRoot";
        group.transform = DMFTransform.fromTransform(Transform.fromRotation(0, -90, 0));
        scene.models.add(group);
        DMFNode node = toModel(monitor, core, object, resourceName);
        group.children.add(node);
    }

    private void exportObjectCollection(
        ProgressMonitor monitor,
        CoreBinary core,
        RTTIObject object,


        @NotNull String resourceName
    ) throws IOException {
        Transform transform = Transform.fromRotation(0, -90, 0);
        DMFModelGroup group = new DMFModelGroup();
        group.name = resourceName;
        group.transform = DMFTransform.fromTransform(transform);
        scene.models.add(group);
        int itemId = 0;
        RTTIReference[] objects = object.get("Objects");
        try (ProgressMonitor.Task task = monitor.begin("Exporting ObjectCollection Objects", objects.length)) {
            for (RTTIReference rttiReference : objects) {
                RTTIReference.FollowResult refObject = rttiReference.follow(core, manager, registry);
                DMFNode node = toModel(task.split(1), refObject.binary(), refObject.object(), nameFromReference(rttiReference, "%s_Object_%d".formatted(resourceName, itemId)));
                group.children.add(node);
                itemId++;
            }
        }
    }

    private DMFNode artPartsSubModelResourceToModel(
        ProgressMonitor monitor,
        CoreBinary core,
        RTTIObject object,


        String resourceName
    ) throws IOException {
        RTTIReference meshResourceRef = object.ref("MeshResource");
        DMFNode model;
        DMFCollection subModelResourceCollection = scene.createCollection(resourceName, collectionStack.peek(), !object.bool("IsHideDefault"));
        collectionStack.push(subModelResourceCollection);

        if (object.ref("ExtraResource").type() != RTTIReference.Type.NONE) {
            RTTIReference.FollowResult extraResourceRef = object.ref("ExtraResource").follow(core, manager, registry);
            if (extraResourceRef.object().type().getTypeName().equals("ArtPartsCoverModelResource") |
                extraResourceRef.object().type().getTypeName().equals("ArtPartsCoverAndAnimResource")) {
                RTTIObject repSkeleton = object.ref("Skeleton").follow(core, manager, registry).object();
                RTTIObject[] defaultPos = extraResourceRef.object().get("DefaultPoseTranslations");
                RTTIObject[] defaultRot = extraResourceRef.object().get("DefaultPoseRotations");
                DMFSkeleton skeleton = new DMFSkeleton();
                final RTTIObject[] joints = repSkeleton.get("Joints");
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
        }

        if (meshResourceRef.type() != RTTIReference.Type.NONE) {
            RTTIReference.FollowResult meshResourceRes = meshResourceRef.follow(core, manager, registry);
            try (ProgressMonitor.Task task = monitor.begin("Exporting ArtPartsSubModelResource MeshResource", 1)) {
                model = toModel(task.split(1), meshResourceRes.binary(), meshResourceRes.object(), nameFromReference(meshResourceRef, resourceName));
            }
        } else {
            model = new DMFModelGroup();
            model.name = resourceName;
        }

        RTTIReference extraMeshResourceRef = object.ref("ExtraResource");
        if (extraMeshResourceRef.type() != RTTIReference.Type.NONE) {
            RTTIReference.FollowResult extraMeshResourceRes = extraMeshResourceRef.follow(core, manager, registry);
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
        ProgressMonitor monitor,
        CoreBinary core,
        RTTIObject object,


        String resourceName
    ) throws IOException {
        RTTIReference meshResourceRef = object.ref("ArtPartsSubModelPartResource");
        DMFNode model;
        DMFCollection subModelPartsCollection = scene.createCollection(resourceName, collectionStack.peek(), !object.bool("IsHideDefault"));
        collectionStack.push(subModelPartsCollection);
        if (meshResourceRef.type() != RTTIReference.Type.NONE) {
            try (ProgressMonitor.Task artPartTask = monitor.begin("Exporting ArtPartsSubModelWithChildrenResource", 1)) {
                RTTIReference.FollowResult meshResourceRes = meshResourceRef.follow(core, manager, registry);
                model = toModel(artPartTask.split(1), meshResourceRes.binary(), meshResourceRes.object(), nameFromReference(meshResourceRef, resourceName));
            }
        } else {
            model = new DMFModelGroup();
            model.name = resourceName;
        }
        RTTIReference[] children = object.get("Children");
        if (children.length > 0) {
            try (ProgressMonitor.Task task = monitor.begin("Exporting Children", children.length)) {
                for (int i = 0; i < children.length; i++) {
                    RTTIReference subPart = children[i];
                    RTTIReference.FollowResult subPartRes = subPart.follow(core, manager, registry);
                    model.children.add(toModel(task.split(1), subPartRes.binary(), subPartRes.object(), nameFromReference(subPart, "child%d_%s".formatted(i, resourceName))));
                }
            }
        }

        model.addToCollection(subModelPartsCollection, scene);
        collectionStack.pop();
        return model;
    }

    private DMFNode modelPartResourceToModel(
        ProgressMonitor monitor,
        CoreBinary core,
        RTTIObject object,


        String resourceName
    ) throws IOException {
        RTTIReference meshResourceRef = object.ref("MeshResource");
        RTTIReference.FollowResult meshResource = meshResourceRef.follow(core, manager, registry);
        try (ProgressMonitor.Task task = monitor.begin("Exporting ModelPartResource MeshResource", 1)) {
            return toModel(task.split(1), meshResource.binary(), meshResource.object(), nameFromReference(meshResourceRef, resourceName));
        }
    }

    private DMFNode prefabResourceToModel(
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,


        @NotNull String resourceName
    ) throws IOException {
        RTTIReference objectCollection = object.ref("ObjectCollection");
        RTTIReference.FollowResult prefabResource = objectCollection.follow(core, manager, registry);
        try (ProgressMonitor.Task task = monitor.begin("Exporting PrefabResource ObjectCollection", 1)) {
            return toModel(task.split(1), prefabResource.binary(), prefabResource.object(), nameFromReference(objectCollection, resourceName));
        }
    }

    private DMFNode prefabInstanceToModel(
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,


        @NotNull String resourceName
    ) throws IOException {
        RTTIReference prefab = object.ref("Prefab");
        RTTIReference.FollowResult prefabResource = prefab.follow(core, manager, registry);
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
        Transform transform = worldTransformToMatrix(object.get("Orientation"));
        node.transform = DMFTransform.fromTransform(transform);
        return node;
    }

    private DMFNode staticMeshInstanceToModel(
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,


        @NotNull String resourceName
    ) throws IOException {
        RTTIReference resource = object.ref("Resource");
        RTTIReference.FollowResult meshResource = resource.follow(core, manager, registry);
        try (ProgressMonitor.Task task = monitor.begin("Exporting StaticMeshInstance Resource", 1)) {
            return toModel(task.split(1), meshResource.binary(), meshResource.object(), nameFromReference(resource, resourceName));
        }
    }

    private DMFNode objectCollectionToModel(
        ProgressMonitor monitor,
        CoreBinary core,
        RTTIObject object,
        String resourceName
    ) throws IOException {
        RTTIReference[] objects = object.get("Objects");
        DMFModelGroup group = new DMFModelGroup();
        group.name = "Collection %s".formatted(resourceName);
        int itemId = 0;
        try (ProgressMonitor.Task task = monitor.begin("Exporting ObjectCollection Objects", objects.length)) {
            for (RTTIReference rttiReference : objects) {
                RTTIReference.FollowResult refObject = rttiReference.follow(core, manager, registry);
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
        ProgressMonitor monitor,
        CoreBinary core,
        RTTIObject object,

        String resourceName
    ) throws IOException {
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

    private DMFNode multiMeshResourceToModel(
        ProgressMonitor monitor,
        CoreBinary core,
        RTTIObject object,


        String resourceName
    ) throws IOException {
        DMFModelGroup group = new DMFModelGroup();
        RTTIObject[] parts = object.get("Parts");
        try (ProgressMonitor.Task task = monitor.begin("Exporting MultiMeshResource Parts", parts.length)) {
            for (int partId = 0; partId < parts.length; partId++) {
                RTTIObject part = parts[partId];
                RTTIReference meshRef = part.ref("Mesh");
                final var mesh = meshRef.follow(core, manager, registry);
                Transform transform = worldTransformToMatrix(part.obj("Transform"));
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
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,


        @NotNull String resourceName
    ) throws IOException {
        Transform transform = Transform.fromRotation(0, -90, 0);
        DMFNode sceneRoot = new DMFModelGroup("SceneRoot");
        sceneRoot.transform = DMFTransform.fromTransform(transform);
        scene.models.add(sceneRoot);

        DMFModel model;
        try (ProgressMonitor.Task task = monitor.begin("Exporting RegularSkinnedMeshResource mesh", 1)) {
            model = regularSkinnedMeshResourceToModel(task.split(1), core, object, resourceName);
        }
        if (model != null) {
            sceneRoot.children.add(model);
        }
    }

    private DMFModel regularSkinnedMeshResourceToModel(
        ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,


        @NotNull String resourceName
    ) throws IOException {
        DrawFlags flags = DrawFlags.fromDataAndRegistry(object.obj("DrawFlags").i32("Data"), registry);
        if (!flags.renderType().equals("Normal")) {
            return null;
        }

        DMFModel model = new DMFModel();
        DMFMesh mesh = new DMFMesh();
        if (object.type().getTypeName().equals("RegularSkinnedMeshResource")) {
            DMFSkeleton skeleton = new DMFSkeleton();
            final RTTIObject skeletonObj = object.ref("Skeleton").follow(core, manager, registry).object();
            final RTTIObject meshJointBindings = object.ref("SkinnedMeshJointBindings").follow(core, manager, registry).object();


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
                        matrix = DMFTransform.fromMatrix(InvertedMatrix4x4TransformToMatrix(inverseBindMatrices[localBoneId]));
                    } else {
                        matrix = masterBone.transform;
                        localSpace = true;
                    }
                } else {
                    matrix = DMFTransform.fromMatrix(InvertedMatrix4x4TransformToMatrix(inverseBindMatrices[localBoneId]));
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
                model.boneRemapTable.put(targetId, (short) skeleton.findBoneId(targetBone.str("Name")));
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
        if (exportSettings.embedBuffers) {
            buffer = new DMFInternalBuffer(dataSource);
        } else {
            String bufferFileName = "%s.dbuf".formatted(resourceName);
            buffer = new DMFExternalBuffer(bufferFileName, dataSource.remaining());
            Files.write(getBuffersPath().resolve(bufferFileName), dataSource.array());
        }
        buffer.originalName = dataSourceLocation;
        Map<RTTIObject, Map.Entry<Integer, Integer>> bufferOffsets = new HashMap<>();

        RTTIReference[] primitivesRefs = object.get("Primitives");
        RTTIReference[] shadingGroupsRefs = object.get("ShadingGroups");
        if (primitivesRefs.length != shadingGroupsRefs.length) {
            throw new IllegalStateException("Primitives count does not match ShadingGroups count!");
        }
        int dataSourceOffset = 0;
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
            try (ProgressMonitor.Task exportTask = task.split(1).begin("Exporting primitives", primitivesRefs.length)) {
                for (int i = 0; i < primitivesRefs.length; i++) {
                    RTTIReference primitivesRef = primitivesRefs[i];
                    RTTIReference shadingGroupRef = shadingGroupsRefs[i];
                    final var primitiveRes = primitivesRef.follow(core, manager, registry);
                    RTTIObject primitiveObj = primitiveRes.object();
                    RTTIObject shadingGroupObj = shadingGroupRef.follow(core, manager, registry).object();
                    RTTIObject vertexArray = primitiveObj.ref("VertexArray").follow(primitiveRes.binary(), manager, registry).object();
                    RTTIObject indexArray = primitiveObj.ref("IndexArray").follow(primitiveRes.binary(), manager, registry).object();
                    RTTIObject vertexArrayUUID = vertexArray.get("ObjectUUID");
                    RTTIObject indicesArrayUUID = indexArray.get("ObjectUUID");

                    final var vertices = vertexArray.obj("Data");
                    final var indices = indexArray.obj("Data");

                    final int vertexCount = vertices.i32("VertexCount");
                    final int indexCount = indices.i32("IndexCount");
                    final int indexStartIndex = primitiveObj.i32("StartIndex");
                    final int indexEndIndex = primitiveObj.i32("EndIndex");
                    final DMFPrimitive primitive = mesh.newPrimitive();
                    primitive.vertexCount = vertexCount;
                    primitive.vertexType = DMFVertexType.SINGLEBUFFER;
                    primitive.vertexStart = 0;
                    primitive.vertexEnd = vertexCount;
                    Map.Entry<Integer, Integer> offsetAndGroupId = bufferOffsets.get(vertexArrayUUID);
                    dataSourceOffset = offsetAndGroupId.getKey();
                    for (RTTIObject stream : vertices.<RTTIObject[]>get("Streams")) {
                        final int stride = stream.i32("Stride");
                        DMFBufferView bufferView = new DMFBufferView();

                        bufferView.offset = dataSourceOffset;
                        bufferView.size = stride * vertexCount;
                        bufferView.setBuffer(buffer, scene);
                        RTTIObject[] elements = stream.get("Elements");
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
                            attribute.semantic = descriptor.semantic;
                            attribute.size = realElementSize;
                            attribute.elementType = storageType.getTypeName();
                            attribute.elementCount = realElementSize / storageType.getSize();
                            attribute.stride = stride;
                            attribute.setBufferView(bufferView, scene);
                            primitive.vertexAttributes.put(descriptor.semantic, attribute);
                        }
                        dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
                    }
                    int indexSize = switch (indices.str("Format")) {
                        case "Index16" -> 2;
                        case "Index32" -> 4;
                        default -> throw new IllegalStateException("Unexpected value: " + indices.str("Format"));
                    };
                    primitive.indexSize = indexSize;
                    primitive.indexCount = indexCount;
                    primitive.indexStart = indexStartIndex;
                    primitive.indexEnd = indexEndIndex;
                    DMFBufferView bufferView = new DMFBufferView();
                    offsetAndGroupId = bufferOffsets.get(indicesArrayUUID);
                    bufferView.offset = offsetAndGroupId.getKey();
                    primitive.groupingId = offsetAndGroupId.getValue();
                    bufferView.size = indexSize * indexCount;
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
        }
        model.name = resourceName;
        model.mesh = mesh;
        model.addToCollection(collectionStack.peek(), scene);
        return model;
    }

    private void exportMaterial(ProgressMonitor monitor, RTTIObject shadingGroup, DMFMaterial material, CoreBinary binary) throws IOException {
        RTTIReference renderEffectRef = shadingGroup.ref("RenderEffect");
        if (renderEffectRef.type() == RTTIReference.Type.NONE) {
            return;
        }
        RTTIReference.FollowResult renderEffectRes = renderEffectRef.follow(binary, manager, registry);
        RTTIObject renderEffect = renderEffectRes.object();
        material.type = renderEffect.str("EffectType");
        if (!exportSettings.exportTextures) {
            return;
        }
        final RTTIObject[] techniqueSets = renderEffect.get("TechniqueSets");
        try (ProgressMonitor.Task task = monitor.begin("Exporting TechniqueSets", techniqueSets.length)) {
            for (RTTIObject techniqueSet : techniqueSets) {
                final RTTIObject[] renderTechniques = techniqueSet.get("RenderTechniques");
                try (ProgressMonitor.Task techSetTask = task.split(1).begin("Exporting RenderTechniques", renderTechniques.length)) {
                    for (RTTIObject renderTechnique : renderTechniques) {
                        final RTTIObject[] textureBindings = renderTechnique.get("TextureBindings");
                        try (ProgressMonitor.Task bindingTask = techSetTask.split(1).begin("Exporting TechniqueSets", textureBindings.length)) {
                            for (RTTIObject textureBinding : textureBindings) {
                                RTTIReference textureRef = textureBinding.ref("TextureResource");
                                if (textureRef.type() == RTTIReference.Type.NONE) {
                                    continue;
                                }
//                              long bindingNameHash = textureBinding.<RTTIObject>get("TextureBindingHandle").i64("Handle");
                                long bindingNameHash = textureBinding.i32("BindingNameHash");
                                String textureUsageName;
//                              if (DMFTextureUsage.contains(bindingNameHash)) {
//                                  textureUsageName = DMFTextureUsage.fromInt(bindingNameHash).name();
//                              } else {
                                textureUsageName = "Texture_%d".formatted(bindingNameHash);
//                              }

                                RTTIReference.FollowResult textureRes = textureRef.follow(binary, manager, registry);
                                RTTIObject texture = textureRes.object();
                                if (texture.type().getTypeName().equals("Texture")) {
                                    String textureName = nameFromReference(textureRef, uuidToString(texture.get("ObjectUUID")));
                                    log.debug("Extracting \"{}\" texture", textureName);
                                    bindingTask.worked(1);

                                    if (scene.getTexture(textureName) != null) {
                                        int textureId2 = scene.textures.indexOf(scene.getTexture(textureName));
                                        if (!material.textureIds.containsValue(textureId2)) {
                                            material.textureIds.put(textureUsageName, textureId2);
                                        }
                                        continue;

                                    }
                                    DMFTexture dmfTexture = exportTexture(texture, textureName);
                                    if (dmfTexture == null) {
                                        dmfTexture = DMFTexture.nonExportableTexture(textureName);
                                    }
                                    dmfTexture.usageType = bindingNameHash;
                                    material.textureIds.put(textureUsageName, scene.textures.indexOf(dmfTexture));

                                } else if (texture.type().getTypeName().equals("TextureSet")) {
                                    RTTIObject[] entries = texture.get("Entries");
                                    try (ProgressMonitor.Task textureSetTask = bindingTask.split(1).begin("Exporting TextureSet entries", entries.length)) {
                                        for (int i = 0; i < entries.length; i++) {
                                            String textureName = "%s_%d".formatted(nameFromReference(textureRef, uuidToString(texture.get("ObjectUUID"))), i);
                                            textureSetTask.worked(1);

                                            if (scene.getTexture(textureName) != null) {
                                                int textureId2 = scene.textures.indexOf(scene.getTexture(textureName));
                                                if (!material.textureIds.containsValue(textureId2)) {
                                                    material.textureIds.put(textureUsageName, textureId2);
                                                }
                                                continue;
                                            }

                                            RTTIObject entry = entries[i];
                                            RTTIReference textureSetTextureRef = entry.ref("Texture");
                                            if (textureSetTextureRef.type() == RTTIReference.Type.NONE) {
                                                continue;
                                            }
                                            log.debug("Extracting \"{}\" {}/{} texture from TextureSet", textureName, i + 1, entries.length);
                                            RTTIReference.FollowResult follow = textureSetTextureRef.follow(textureRes.binary(), manager, registry);
                                            RTTIObject textureSetTexture = follow.object();

                                            DMFTexture dmfTexture = exportTexture(textureSetTexture, textureName);
                                            if (dmfTexture == null) {
                                                dmfTexture = DMFTexture.nonExportableTexture(textureName);
                                            }
                                            dmfTexture.usageType = bindingNameHash;
                                            int usageInfo = entry.i32("PackingInfo");
                                            dmfTexture.metadata.put("R", PackingInfoHandler.getInfo(usageInfo & 0xFF));
                                            dmfTexture.metadata.put("G", PackingInfoHandler.getInfo(usageInfo >>> 8 & 0xff));
                                            dmfTexture.metadata.put("B", PackingInfoHandler.getInfo(usageInfo >>> 16 & 0xff));
                                            dmfTexture.metadata.put("A", PackingInfoHandler.getInfo(usageInfo >>> 24 & 0xff));

                                            material.textureIds.put(textureUsageName, scene.textures.indexOf(dmfTexture));

                                        }
                                    }
                                } else {
                                    log.warn("Texture of type {} not supported", texture.type().getTypeName());
                                    bindingTask.worked(1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private DMFTexture exportTexture(RTTIObject texture, String textureName) throws IOException {

        switch (texture.type().getTypeName()) {
            case "Texture":
                break;
            case "TextureList":
                texture = texture.<RTTIObject[]>get("Textures")[0];
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
            DMFInternalTexture dmfInternalTexture = new DMFInternalTexture();
            dmfInternalTexture.bufferData = Base64.getEncoder().encodeToString(src);
            dmfInternalTexture.bufferSize = src.length;
            dmfTexture = dmfInternalTexture;
        } else {
            DMFExternalTexture dmfExternalTexture = new DMFExternalTexture();
            dmfExternalTexture.bufferSize = src.length;
            dmfExternalTexture.bufferFileName = textureName + ".png";
            Files.write(getBuffersPath().resolve(textureName + ".png"), src);
            dmfTexture = dmfExternalTexture;
        }
        dmfTexture.dataType = DMFDataType.PNG;
        dmfTexture.name = textureName;

        scene.textures.add(dmfTexture);
        return dmfTexture;
    }


    private record AccessorDescriptor(@NotNull String semantic, @NotNull ElementType elementType, @NotNull ComponentType componentType, boolean unsigned, boolean normalized) {}

    private static class DrawFlags {
        public boolean castShadow;
        public String renderType;
        public String shadowCullMode;
        public String viewLayer;
        public float shadowBiasMultiplier;
        public String shadowBiasMode;
        public boolean disableOcclusionCulling;
        public boolean voxelizeLightBake;

        public DrawFlags() {
        }

        public static DrawFlags fromDataAndRegistry(int flags, RTTITypeRegistry registry) {
            RTTITypeEnum eDrawPartType = ((RTTITypeEnum) registry.find("EDrawPartType"));
            RTTITypeEnum eShadowCull = ((RTTITypeEnum) registry.find("EShadowCull"));
            RTTITypeEnum eViewLayer = ((RTTITypeEnum) registry.find("EViewLayer"));
            RTTITypeEnum eShadowBiasMode = ((RTTITypeEnum) registry.find("EShadowBiasMode"));

            DrawFlags drawFlags = new DrawFlags();
            drawFlags.castShadow = (flags & 1) > 0;
            drawFlags.renderType = eDrawPartType.valueOf((flags >>> 3) & 1).name();
            drawFlags.shadowCullMode = eShadowCull.valueOf((flags >>> 1) & 3).name();
            drawFlags.viewLayer = eViewLayer.valueOf((flags >>> 4) & 3).name();
            drawFlags.shadowBiasMultiplier = MathUtils.toFloat((short) ((flags >>> 6) & 65535));
            drawFlags.shadowBiasMode = eShadowBiasMode.valueOf((flags >>> 22) & 1).name();
            drawFlags.disableOcclusionCulling = ((flags >>> 24) & 1) > 0;
            drawFlags.voxelizeLightBake = (flags & 0x2000000) > 0;
            return drawFlags;
        }

        public boolean castShadow() {
            return castShadow;
        }

        public String renderType() {
            return renderType;
        }

        public String shadowCullMode() {
            return shadowCullMode;
        }

        public String viewLayer() {
            return viewLayer;
        }

        public float shadowBiasMultiplier() {
            return shadowBiasMultiplier;
        }

        public String shadowBiasMode() {
            return shadowBiasMode;
        }

        public boolean disableOcclusionCulling() {
            return disableOcclusionCulling;
        }

        public boolean voxelizeLightBake() {
            return voxelizeLightBake;
        }

    }

}
