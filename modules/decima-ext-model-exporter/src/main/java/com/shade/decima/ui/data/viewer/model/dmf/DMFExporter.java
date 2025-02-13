package com.shade.decima.ui.data.viewer.model.dmf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.messages.ds.DSIndexArrayResourceHandler;
import com.shade.decima.model.rtti.messages.ds.DSVertexArrayResourceHandler;
import com.shade.decima.model.rtti.messages.hzd.HZDIndexArrayResourceHandler;
import com.shade.decima.model.rtti.messages.hzd.HZDVertexArrayResourceHandler;
import com.shade.decima.model.rtti.messages.shared.VertexStream;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.ds.DSDataSource;
import com.shade.decima.model.rtti.types.hzd.HZDDataSource;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.handlers.PackingInfoHandler;
import com.shade.decima.ui.data.viewer.model.BaseModelExporter;
import com.shade.decima.ui.data.viewer.model.ModelExporter;
import com.shade.decima.ui.data.viewer.model.ModelExporterProvider;
import com.shade.decima.ui.data.viewer.model.dmf.data.*;
import com.shade.decima.ui.data.viewer.model.dmf.nodes.*;
import com.shade.decima.ui.data.viewer.model.dmf.serializers.*;
import com.shade.decima.ui.data.viewer.texture.TextureViewer;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.exporter.TextureExporterPNG;
import com.shade.decima.ui.data.viewer.texture.exporter.TextureExporterTIFF;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

public class DMFExporter extends BaseModelExporter implements ModelExporter {
    private static final Logger log = LoggerFactory.getLogger(DMFExporter.class);
    private static final Map<String, String> SEMANTICS_REMAP = Map.ofEntries(
        Map.entry("Pos", "POSITION"),
        Map.entry("TangentBFlip", "TANGENT"),
        Map.entry("Tangent", "TANGENT"),
        Map.entry("Normal", "NORMAL"),
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
    private final Gson gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(List.class, new JsonListSerializer())
        .registerTypeHierarchyAdapter(DMFBuffer.class, new JsonBufferSerializer(this))
        .registerTypeAdapter(DMFTransform.class, new JsonTransformSerializer())
        .registerTypeAdapter(Vector3fc.class, new JsonVector3fcSerializer())
        .registerTypeAdapter(Vector2ic.class, new JsonVector2icSerializer())
        .create();
    private final Project project;
    private final Set<ModelExporterProvider.Option> options;
    private final Path output;
    private final Stack<DMFCollection> collectionStack = new Stack<>();
    private final Map<RTTIObject, Integer> instances = new HashMap<>();
    private final Map<Point, DMFTileData> tiles = new HashMap<>();
    private int depth = 0;
    private DMFSceneFile scene;
    private DMFSkeleton masterSkeleton;

    public DMFExporter(@NotNull Project project, @NotNull Set<ModelExporterProvider.Option> options, @NotNull Path output) {
        this.project = project;
        this.options = options;
        this.output = output;
    }

    @Override
    public void export(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull SeekableByteChannel channel
    ) throws IOException {
        final DMFSceneFile scene = export(monitor, file, object, IOUtils.getBasename(output));

        try (Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8)) {

            for (DMFTileData tile : tiles.values()) {
                generateMapTileNode(tile);
            }

            gson.toJson(scene, scene.getClass(), createJsonWriter(writer));
        }
    }

    private void generateMapTileNode(DMFTileData tileData) {
        DMFMapTile mapTile = new DMFMapTile("Tile_%d_%d".formatted(tileData.gridCoordinate.x, tileData.gridCoordinate.y));
        mapTile.textures.putAll(tileData.textures);
        mapTile.bboxMin = new Vector3f(tileData.bboxMin.x(), tileData.bboxMin.y(), tileData.bboxMin.z());
        mapTile.bboxMax = new Vector3f(tileData.bboxMax.x(), tileData.bboxMax.y(), tileData.bboxMax.z());
        mapTile.gridCoordinate = new Vector2i(tileData.gridCoordinate.x, tileData.gridCoordinate.y);
        scene.models.add(mapTile);
    }

    @NotNull
    private DMFSceneFile export(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        scene = new DMFSceneFile(2);
        collectionStack.push(scene.createCollection(resourceName));
        exportResource(monitor, file, object, resourceName);
        collectionStack.pop();
        return scene;
    }

    private int toInstanceSource(@NotNull String uuid, @NotNull DMFNode node) {
        DMFInstanceSource instanceSource = new DMFInstanceSource(uuid, node);
        scene.instances.add(instanceSource);
        return scene.instances.indexOf(instanceSource);
    }


    private void exportResource(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        log.info("Exporting {}", object.type().getTypeName());
        switch (object.type().getTypeName()) {
            case "LevelDataGame" -> exportLevelDataGame(monitor, file, object, resourceName);
            case "Terrain" -> exportTerrain(monitor, file, object, resourceName);
            case "ControlledEntityResource" -> exportControlledEntityResource(monitor, file, object, resourceName);
            case "ArtPartsDataResource" -> exportArtPartsDataResource(monitor, file, object, resourceName);
            case "TileBasedStreamingStrategyResource", "SkinnedModelResource", "StreamingTileResource",
                 "LodMeshResource", "MultiMeshResource", "ModelPartResource", "ObjectCollection",
                 "RegularSkinnedMeshResource", "StaticMeshResource" ->
                exportModelGeneric(monitor, file, object, resourceName);
            default -> throw new IllegalArgumentException("Unsupported resource: " + object.type());
        }
    }

    private void exportModelGeneric(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final DMFNode node = toModel(monitor, core, object, resourceName);
        if (node != null && !node.isEmpty()) {
            scene.models.add(node);
        }
    }

    private void exportControlledEntityResource(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIReference.FollowResult modelResourceRef = object.ref("ModelResource").follow(project, file);
        if (modelResourceRef == null) {
            return;
        }
        final DMFNode model;
        try (ProgressMonitor.Task task = monitor.begin("Exporting ControlledEntity", 3)) {
            model = toModel(task.split(1), modelResourceRef.file(), modelResourceRef.object(), object.str("Name"));
            if (model == null) {
                return;
            }

            final RTTIReference.FollowResult destructibilityResourceRef = object.ref("DestructibilityResource").follow(project, file);
            if (destructibilityResourceRef == null) {
                return;
            }

            final RTTIReference.FollowResult defaultDamagePartRef = destructibilityResourceRef.object().ref("DefaultDamagePart").follow(project, destructibilityResourceRef.file());
            if (defaultDamagePartRef != null) {
                DMFAttachment attachment = destructibilityPartToModel(task.split(1), defaultDamagePartRef.file(), defaultDamagePartRef.object(), resourceName);
                if (attachment != null) {
                    model.children.add(attachment);
                }
            } else {
                task.worked(1);
            }

            final var convertedParts = destructibilityResourceRef.object().refs("ConvertedParts");
            try (ProgressMonitor.Task cpTask = task.split(1).begin("Exporting ControlledEntity ConvertedParts", convertedParts.length)) {
                for (RTTIReference part : convertedParts) {
                    final RTTIReference.FollowResult partRef = part.follow(project, destructibilityResourceRef.file());
                    if (partRef == null) {
                        return;
                    }
                    DMFNode partNode = toModel(cpTask.split(1), partRef.file(), partRef.object(), resourceName);
                    if (partNode != null) {
                        model.children.add(partNode);
                    }
                }
            }
        }

        scene.models.add(model);
    }

    private void exportLevelDataGame(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIReference[] strategyResources = object.get("StrategyResources");
        try (ProgressMonitor.Task levelDataTask = monitor.begin("Exporting LevelDataGame", strategyResources.length)) {
            final DMFModelGroup group = new DMFModelGroup(resourceName);
            scene.models.add(group);
            for (int i = 0; i < strategyResources.length; i++) {
                final RTTIReference ref = strategyResources[i];
                final RTTIReference.FollowResult strategyResourcesRes = ref.follow(project, file);
                if (strategyResourcesRes == null) {
                    continue;
                }
                DMFNode node = toModel(levelDataTask.split(1), strategyResourcesRes.file(), strategyResourcesRes.object(), nameFromReference(ref, "%s_StrtRes_%d".formatted(resourceName, i)));
                if (node != null) {
                    group.children.add(node);
                }
            }
        }
    }

    private void exportTerrain(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        try (ProgressMonitor.Task terrainTask = monitor.begin("Exporting Terrain", 1)) {
            final RTTIObject shadingPassSetup = object.obj("ShadingPassSetup");

            final DMFMesh mesh = new DMFMesh();
            final DMFModel model = new DMFModel(resourceName, mesh);

            final RTTIObject vertexArrayObj = Objects.requireNonNull(shadingPassSetup.ref("VertexArray").get(project, file));
            final RTTIReference[] indexArrays = shadingPassSetup.get("IndexArrays");
            final RTTIObject indexArrayObj = Objects.requireNonNull(indexArrays[0].get(project, file));
            final HZDVertexArrayResourceHandler.HwVertexArray vertices = vertexArrayObj.obj("Data").cast();
            final HZDIndexArrayResourceHandler.HwIndexArray indices = indexArrayObj.obj("Data").cast();

            final DMFPrimitive primitive = new DMFPrimitive(0, vertices.vertexCount, DMFVertexBufferType.SINGLE_BUFFER, 0, vertices.vertexCount,
                indices.getIndexSize(), indices.indexCount, 0, indices.indexCount
            );
            mesh.primitives.add(primitive);
            int vertexStreamOffset = -1;
            for (RTTIObject streamObj : vertices.streams) {
                HZDVertexArrayResourceHandler.HwVertexStream stream = streamObj.cast();
                final int stride = stream.stride;

                final DMFBuffer buffer;
                long resourceLength = 0;
                if (stream.dataSource == null) {
                    buffer = new DMFInternalBuffer("INTERNAL", new DMFBuffer.ByteArrayDataProvider(stream.data));
                    scene.buffers.add(buffer);
                } else {
                    final HZDDataSource dataSource = stream.dataSource.cast();
                    if (vertexStreamOffset == -1) {
                        vertexStreamOffset = dataSource.getOffset();
                    }

                    final String dataSourceLocation = dataSource.location.substring(dataSource.location.indexOf(":") + 1);
                    buffer = createDataBuffer(dataSourceLocation, dataSource, vertexStreamOffset, stride * vertices.vertexCount);
                    resourceLength = (long) stride * vertices.vertexCount;

                }
                convertVertexAttributes(vertices.vertexCount, 0, primitive, stream, stride, buffer);
                vertexStreamOffset += resourceLength;
            }

            final DMFBuffer buffer;
            if (indices.dataSource == null) {
                buffer = new DMFInternalBuffer("INTERNAL", new DMFBuffer.ByteArrayDataProvider(indices.data));
                scene.buffers.add(buffer);
            } else {
                final HZDDataSource dataSource = indices.dataSource.cast();
                final String dataSourceLocation = dataSource.location.substring(dataSource.location.indexOf(":") + 1);
                buffer = createDataBuffer(dataSourceLocation, dataSource,
                    vertexStreamOffset, indices.getIndexSize() * indices.indexCount
                );
            }

            final DMFBufferView bufferView = new DMFBufferView(scene.buffers.indexOf(buffer), 0, indices.getIndexSize() * indices.indexCount);
            primitive.setIndexBufferView(bufferView, scene);

            RTTIObject lodDistance = shadingPassSetup.<RTTIObject[]>get("LODDistances")[0];
            int materialId = lodDistance.i32("MaterialLayerIndex");
            RTTIObject materialLayer = shadingPassSetup.<RTTIObject[]>get("MaterialLayers")[materialId];
            RTTIReference.FollowResult renderEffectResourceRef = materialLayer.ref("RenderEffectResource").follow(project, file);
            final DMFMaterial material;
            if (renderEffectResourceRef == null) {
                material = new DMFMaterial("Terrain_%s".formatted(RTTIUtils.uuidToString(object.uuid())));
                terrainTask.worked(1);
            } else {
                RTTIObject renderEffectResource = renderEffectResourceRef.object();
                final RTTIObject materialUUID = renderEffectResource.uuid();
                final String materialName = RTTIUtils.uuidToString(materialUUID);
                if (scene.getMaterial(materialName) == null) {
                    material = scene.createMaterial("Terrain_%s".formatted(materialName));
                    exportMaterial(terrainTask.split(1), renderEffectResource, material, file);
                } else {
                    material = scene.getMaterial(materialName);
                    terrainTask.worked(1);
                }
            }
            primitive.setMaterial(material, scene);


            final Matrix4dc transform = worldTransformToMatrix(object.get("Orientation"));
            model.transform = new DMFTransform(transform);

            scene.models.add(model);
        }
    }

    private void convertVertexAttributes(int vertexCount, int dataSourceOffset, DMFPrimitive primitive, VertexStream stream, int stride, DMFBuffer buffer) {
        final DMFBufferView bufferView = new DMFBufferView(scene.buffers.indexOf(buffer), dataSourceOffset, stride * vertexCount);

        final RTTIObject[] elements = stream.elements();
        for (int j = 0; j < elements.length; j++) {
            final RTTIObject element = elements[j];
            final int offset = element.i8("Offset");
            int realElementSize = 0;
            if (j < elements.length - 1) {
                realElementSize = elements[j + 1].i8("Offset") - offset;
            } else if (j == 0) {
                realElementSize = stride;
            } else if (j == elements.length - 1) {
                realElementSize = stride - offset;
            }
            final String elementType = element.str("Type");
            final String semantic = SEMANTICS_REMAP.get(elementType);
            if (semantic == null) {
                continue;
            }
            final DMFVertexAttribute attribute = new DMFVertexAttribute();
            final DMFComponentType componentTypea = DMFComponentType.fromString(element.str("StorageType"));
            attribute.offset = offset;
            attribute.semantic = semantic;
            attribute.size = realElementSize;
            attribute.elementType = componentTypea.name();
            attribute.elementCount = realElementSize / componentTypea.getSize();
            attribute.stride = stride;
            attribute.setBufferView(bufferView, scene);
            primitive.vertexAttributes.put(semantic, attribute);
        }
    }

    private void exportArtPartsDataResource(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final DMFNode compositeModel;
        try (ProgressMonitor.Task artPartTask = monitor.begin("Exporting ArtPartsDataResource RootModel", 2)) {
            final RTTIObject repSkeleton = object.ref("RepresentationSkeleton").get(project, file);

            if (repSkeleton != null) {
                masterSkeleton = exportSkeleton(repSkeleton);
                scene.skeletons.add(masterSkeleton);
                compositeModel = new DMFCompositeModel(resourceName, scene.skeletons.indexOf(masterSkeleton));

                final RTTIObject[] defaultPos = object.get("DefaultPoseTranslations");
                final RTTIObject[] defaultRot = object.get("DefaultPoseRotations");
                final RTTIObject[] joints = repSkeleton.objs("Joints");
                for (short i = 0; i < joints.length; i++) {
                    final RTTIObject joint = joints[i];
                    final DMFTransform matrix = poseToMatrix(i, defaultRot, defaultPos);
                    final DMFBone bone = masterSkeleton.findBone(joint.str("Name"));
                    if (bone != null) {
                        bone.transform = matrix;
                    } else {
                        masterSkeleton.newBone(joint.str("Name"), matrix, joint.i16("ParentIndex")).localSpace = true;
                    }
                }
            } else {
                compositeModel = new DMFNode(resourceName);
            }

            try (ProgressMonitor.Task task = artPartTask.split(1).begin("Exporting RootModel", 1)) {
                final RTTIReference.FollowResult rootModelRes = Objects.requireNonNull(object.ref("RootModel").follow(project, file));
                DMFNode model = toModel(task.split(1), rootModelRes.file(), rootModelRes.object(), nameFromReference(object.ref("RootModel"), resourceName));
                if (model != null) {
                    compositeModel.children.add(model);
                }
            }
            RTTIReference[] subModels = object.get("SubModelPartResources");
            try (ProgressMonitor.Task task = artPartTask.split(1).begin("Exporting SubModelPartResources", subModels.length)) {
                for (int i = 0; i < subModels.length; i++) {
                    RTTIReference subPart = subModels[i];
                    RTTIReference.FollowResult subPartRes = Objects.requireNonNull(subPart.follow(project, file));
                    DMFNode node = toModel(task.split(1), subPartRes.file(), subPartRes.object(), "SubModel%d_%s".formatted(i, nameFromReference(subPart, resourceName)));
                    if (node != null) {
                        compositeModel.children.add(node);
                    }
                }
            }
        }
        scene.models.add(compositeModel);
    }

    @Nullable
    private DMFNode toModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        depth += 1;
        log.debug("{}Converting {}", "\t".repeat(depth), object.type().getTypeName());
        final DMFNode res = switch (object.type().getTypeName()) {
            case "TileBasedStreamingStrategyResource" ->
                tileBasedStreamingStrategyResourceToModel(monitor, file, object, resourceName);
            case "AlwaysLoadedStreamingStrategyResource" ->
                alwaysLoadedStreamingStrategyResourceToModel(monitor, file, object, resourceName);
            case "PrefabResource" -> prefabResourceToModel(monitor, file, object, resourceName);
            case "ModelPartResource" -> modelPartResourceToModel(monitor, file, object, resourceName);
            case "SkinnedModelResource" -> skinnedModelResourceToModel(monitor, file, object, resourceName);
            case "ArtPartsSubModelWithChildrenResource" ->
                artPartsSubModelWithChildrenResourceToModel(monitor, file, object, resourceName);
            case "ArtPartsSubModelResource" -> artPartsSubModelResourceToModel(monitor, file, object, resourceName);
            case "PrefabInstance" -> prefabInstanceToModel(monitor, file, object, resourceName);
            case "DestructibilityResource" -> destructibilityResourceToModel(monitor, file, object, resourceName);
            case "DestructibilityPart" -> destructibilityPartToModel(monitor, file, object, resourceName);
            case "DestructibilityPartStateResource" ->
                destructibilityPartStateResourceToModel(monitor, file, object, resourceName);
            case "ObjectCollection" -> objectCollectionToModel(monitor, file, object, resourceName);
            case "StaticMeshInstance" -> staticMeshInstanceToModel(monitor, file, object, resourceName);
            case "StreamingTileResource" -> streamingTileResourceToModel(monitor, file, object, resourceName);
            case "LodMeshResource" -> lodMeshResourceToModel(monitor, file, object, resourceName);
            case "MultiMeshResource" -> multiMeshResourceToModel(monitor, file, object, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                regularSkinnedMeshResourceToModel(monitor, file, object, resourceName);
            case "WorldDataTextureMap" -> worldDataTextureMapToModel(monitor, file, object, resourceName);
            case "TerrainTileData" -> terrainTileDataToModel(monitor, file, object, resourceName);
            default -> {
                log.info("{}Cannot export {}", "\t".repeat(depth), object.type().getTypeName());
                yield null;
            }
        };
        depth -= 1;
        return res;
    }

    private DMFNode terrainTileDataToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) {
        RTTIObject gridCoordinates = object.get("GridCoordinates");
        Point gridPoint = new Point(gridCoordinates.i32("X"), gridCoordinates.i32("Y"));
        DMFTileData tileData = tiles.computeIfAbsent(gridPoint, DMFTileData::new);
        RTTIObject bbox = object.get("BoundingBox");
        tileData.bboxMin = new Vector3f(bbox.obj("Min").f32("X"), bbox.obj("Min").f32("Y"), bbox.obj("Min").f32("Z"));
        tileData.bboxMax = new Vector3f(bbox.obj("Max").f32("X"), bbox.obj("Max").f32("Y"), bbox.obj("Max").f32("Z"));
        tiles.put(gridPoint, tileData);
        return null;
    }

    private DMFNode worldDataTextureMapToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        if (!resourceName.equals("worlddata_height_terrain") && !options.contains(ModelExporterProvider.Option.EXPORT_TEXTURES)){
            return null;
        }
        RTTIObject gridCoordinates = object.get("GridCoordinates");
        Point gridPoint = new Point(gridCoordinates.i32("X"), gridCoordinates.i32("Y"));
        DMFTileData tileData = tiles.computeIfAbsent(gridPoint, DMFTileData::new);

        final RTTIReference.FollowResult resultTextureRef = object.ref("ResultTexture").follow(project, core);
        if (resultTextureRef != null) {
            DMFTexture texture = exportTexture(monitor, resultTextureRef.object(), resourceName + gridPoint.x + "_" + gridPoint.y);
            DMFMapTile.TileTextureInfo textureInfo = new DMFMapTile.TileTextureInfo(scene.textures.indexOf(texture), new HashMap<>());
            for (RTTIReference entryRef : object.refs("Entries")) {
                final RTTIReference.FollowResult entryRefRes = entryRef.follow(project, core);
                if (entryRefRes == null) {
                    continue;
                }
                final RTTIReference.FollowResult typeRef = entryRefRes.object().ref("Type").follow(project, core);
                if (typeRef == null) {
                    continue;
                }
                final RTTIObject typeInfo = typeRef.object();
                final String channel = entryRefRes.object().str("Channel");
                final String usage = typeRef.object().str("Name");
                textureInfo.channels().put(channel, new DMFMapTile.TileTextureChannelInfo(usage,
                    typeInfo.obj("Range").f32("Min"), typeInfo.obj("Range").f32("Max")));
            }
            tileData.textures.put(resourceName, textureInfo);
        }
        tiles.put(gridPoint, tileData);
        return null;
    }

    private DMFNode skinnedModelResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIObject skeleton = Objects.requireNonNull(object.ref("Skeleton").get(project, file), "Skinned model does not have skeleton!");
        masterSkeleton = exportSkeleton(skeleton);
        scene.skeletons.add(masterSkeleton);

        final RTTIReference[] helpers = object.get("Helpers");
        for (RTTIReference helperRef : helpers) {
            final RTTIReference.FollowResult helperRefRes = helperRef.follow(project, file);
            if (helperRefRes != null) {
                exportHelpers(helperRefRes.object(), masterSkeleton, skeleton);
            }
        }

        final DMFCompositeModel sceneRoot = new DMFCompositeModel(resourceName, scene.skeletons.indexOf(masterSkeleton));

        RTTIReference[] subModels = object.get("ModelPartResources");
        try (ProgressMonitor.Task task = monitor.begin("Exporting ModelPartResources", subModels.length)) {
            for (int i = 0; i < subModels.length; i++) {
                RTTIReference subPart = subModels[i];
                RTTIReference.FollowResult subPartRes = Objects.requireNonNull(subPart.follow(project, file));
                DMFNode node = toModel(task.split(1), subPartRes.file(), subPartRes.object(), "SubModel%d_%s".formatted(i, nameFromReference(subPart, subPartRes.object().str("Name"))));
                if (node != null) {
                    sceneRoot.children.add(node);
                }
            }
        }

        return sceneRoot;
    }

    private DMFNode destructibilityResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        DMFModelGroup group = new DMFModelGroup(resourceName);
        RTTIReference[] convertedParts = object.get("ConvertedParts");
        try (ProgressMonitor.Task task = monitor.begin("Exporting DestructibilityResource converted parts", convertedParts.length)) {
            for (RTTIReference convertedPartRef : convertedParts) {
                RTTIReference.FollowResult convertedPartRefRes = convertedPartRef.follow(project, file);
                if (convertedPartRefRes == null) {
                    task.worked(1);
                    continue;
                }
                RTTIObject convertedPart = convertedPartRefRes.object();
                DMFNode node = toModel(task.split(1), convertedPartRefRes.file(), convertedPart, convertedPart.str("Name"));
                if (node != null) {
                    group.children.add(node);
                }
            }
        }
        return group;
    }

    @NotNull
    private DMFNode artPartsSubModelResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIReference meshResourceRef = object.ref("MeshResource");
        final DMFCollection subModelResourceCollection = scene.createCollection(resourceName, collectionStack.peek(), !object.bool("IsHideDefault"));
        collectionStack.push(subModelResourceCollection);

        final RTTIReference extraMeshResourceRef = object.ref("ExtraResource");
        final RTTIReference.FollowResult extraResourceRef = extraMeshResourceRef.follow(project, file);

        if (extraResourceRef != null && (extraResourceRef.object().type().getTypeName().equals("ArtPartsCoverModelResource") ||
            extraResourceRef.object().type().getTypeName().equals("ArtPartsCoverAndAnimResource"))) {
            Objects.requireNonNull(masterSkeleton);
            final RTTIObject extraResource = extraResourceRef.object();
            final RTTIObject skeleton = Objects.requireNonNull(object.ref("Skeleton").follow(project, file)).object();

            validateSkeleton(masterSkeleton, skeleton);

            final RTTIObject[] defaultPos = extraResource.get("DefaultPoseTranslations");
            final RTTIObject[] defaultRot = extraResource.get("DefaultPoseRotations");
            final RTTIObject[] joints = skeleton.objs("Joints");
            for (short i = 0; i < joints.length; i++) {
                final RTTIObject joint = joints[i];
                DMFTransform matrix = poseToMatrix(i, defaultRot, defaultPos);
                final DMFBone bone = masterSkeleton.findBone(joint.str("Name"));
                if (bone != null) {
                    bone.transform = matrix;
                } else {
                    masterSkeleton.newBone(joint.str("Name"), matrix, joint.i16("ParentIndex")).localSpace = true;
                }
            }
        }
        DMFNode model;

        try (ProgressMonitor.Task task = monitor.begin("Exporting ArtPartsSubModelResource", 2)) {
            final RTTIReference.FollowResult meshResourceRes = meshResourceRef.follow(project, file);
            if (meshResourceRes != null) {
                model = toModel(task.split(1), meshResourceRes.file(), meshResourceRes.object(), nameFromReference(meshResourceRef, resourceName));
            } else {
                task.worked(1);
                model = new DMFModelGroup(resourceName);
            }
            if (model == null) {
                model = new DMFNode(resourceName);
            }

            if (extraResourceRef != null) {
                DMFNode extraModel = toModel(task.split(1), extraResourceRef.file(), extraResourceRef.object(), "EXTRA_" + nameFromReference(extraMeshResourceRef, resourceName));
                if (extraModel != null) {
                    model.children.add(extraModel);
                }
            } else {
                task.worked(1);
            }
        }
        model.addToCollection(subModelResourceCollection, scene);
        collectionStack.pop();
        return model;
    }

    private static DMFTransform poseToMatrix(short jointId, RTTIObject[] defaultRot, RTTIObject[] defaultPos) {
        final Quaterniond rotations;
        final Vector3d position;
        if (jointId < defaultRot.length) {
            rotations = new Quaterniond(defaultRot[jointId].f32("X"), defaultRot[jointId].f32("Y"), defaultRot[jointId].f32("Z"), defaultRot[jointId].f32("W"));
        } else {
            rotations = new Quaterniond(0, 0, 0, 1);
        }
        if (jointId < defaultPos.length) {
            position = new Vector3d(defaultPos[jointId].f32("X"), defaultPos[jointId].f32("Y"), defaultPos[jointId].f32("Z"));
        } else {
            position = new Vector3d(0, 0, 0);
        }

        return new DMFTransform(
            position,
            new Vector3d(1, 1, 1),
            rotations
        );
    }

    @Nullable
    private DMFNode artPartsSubModelWithChildrenResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIReference meshResourceRef = object.ref("ArtPartsSubModelPartResource");
        final DMFNode model;
        final DMFCollection subModelPartsCollection = scene.createCollection(resourceName, collectionStack.peek(), !object.bool("IsHideDefault"));
        collectionStack.push(subModelPartsCollection);
        final RTTIReference.FollowResult meshResourceRes = meshResourceRef.follow(project, file);
        try (ProgressMonitor.Task task = monitor.begin("Exporting ArtPartsSubModelWithChildrenResource", 2)) {
            if (meshResourceRes != null) {
                model = toModel(task.split(1), meshResourceRes.file(), meshResourceRes.object(), nameFromReference(meshResourceRef, resourceName));
            } else {
                task.worked(1);
                model = new DMFModelGroup(resourceName);
            }
            if (model == null) {
                return null;
            }
            final RTTIReference[] children = object.get("Children");
            if (children.length > 0) {
                try (ProgressMonitor.Task subTask = task.split(1).begin("Exporting ArtPartsSubModelWithChildrenResource Children", children.length)) {
                    for (int i = 0; i < children.length; i++) {
                        RTTIReference subPart = children[i];
                        RTTIReference.FollowResult subPartRes = Objects.requireNonNull(subPart.follow(project, file));
                        final DMFNode node = toModel(subTask.split(1), subPartRes.file(), subPartRes.object(), nameFromReference(subPart, "child%d_%s".formatted(i, resourceName)));
                        if (node != null) {
                            model.children.add(node);
                        }
                    }
                }
            } else {
                task.worked(1);
            }
        }

        model.addToCollection(subModelPartsCollection, scene);
        collectionStack.pop();
        return model;
    }

    @Nullable
    private DMFNode modelPartResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIReference meshResourceRef = object.ref("MeshResource");
        final RTTIReference.FollowResult meshResource = meshResourceRef.follow(project, file);
        if (meshResource == null) {
            return null;
        }
        try (ProgressMonitor.Task task = monitor.begin("Exporting ModelPartResource MeshResource", 1)) {
            return toModel(task.split(1), meshResource.file(), meshResource.object(), nameFromReference(meshResourceRef, resourceName));
        }
    }

    @Nullable
    private DMFNode tileBasedStreamingStrategyResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final DMFModelGroup group = new DMFModelGroup(resourceName);
        final RTTIReference[] tileRefs = object.get("Tiles");
        try (ProgressMonitor.Task task = monitor.begin("Exporting TileBasedStreamingStrategyResource Tiles", tileRefs.length)) {
            for (int i = 0; i < tileRefs.length; i++) {
                final RTTIReference ref = tileRefs[i];
                final RTTIReference.FollowResult tileRefRes = ref.follow(project, file);
                if (tileRefRes == null) {
                    continue;
                }

                DMFNode node = toModel(task.split(1), tileRefRes.file(), tileRefRes.object(), nameFromReference(ref, "%s_Tile_%d".formatted(resourceName, i)));
                if (node != null) {
                    group.children.add(node);
                }
            }
        }
        return group;
    }

    @Nullable
    private DMFNode alwaysLoadedStreamingStrategyResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final DMFModelGroup group = new DMFModelGroup(resourceName);
        final RTTIReference objectCollectionRef = object.ref("ObjectCollection");
        try (ProgressMonitor.Task task = monitor.begin("Exporting AlwaysLoadedStreamingStrategyResource resources", 1)) {
            final RTTIReference.FollowResult objectCollectionRefRes = objectCollectionRef.follow(project, file);
            if (objectCollectionRefRes == null) {
                return null;
            }
            DMFNode node = toModel(task.split(1), objectCollectionRefRes.file(), objectCollectionRefRes.object(), nameFromReference(objectCollectionRef, resourceName));
            if (node != null) {
                group.children.add(node);
            }
        }
        return group;
    }

    @Nullable
    private DMFNode prefabResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIReference objectCollection = object.ref("ObjectCollection");
        final RTTIReference.FollowResult prefabResource = Objects.requireNonNull(objectCollection.follow(project, file));
        try (ProgressMonitor.Task task = monitor.begin("Exporting PrefabResource ObjectCollection", 1)) {
            return toModel(task.split(1), prefabResource.file(), prefabResource.object(), nameFromReference(objectCollection, resourceName));
        }
    }

    @NotNull
    private DMFNode prefabInstanceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIReference prefab = object.ref("Prefab");
        final RTTIReference.FollowResult prefabResource = Objects.requireNonNull(prefab.follow(project, file));
        final RTTIObject prefabObject = prefabResource.object();
        int instanceId = -1;
        if (instances.containsKey(prefabObject.uuid())) {
            instanceId = instances.get(prefabObject.uuid());
        } else {
            final DMFNode instanceData;
            try (ProgressMonitor.Task task = monitor.begin("Exporting PrefabInstance Prefab", 1)) {
                instanceData = toModel(task.split(1), prefabResource.file(), prefabObject, nameFromReference(prefab, resourceName));
            }
            if (instanceData != null) {
                instanceId = toInstanceSource(RTTIUtils.uuidToString(prefabObject.uuid()), instanceData);
                instances.put(prefabObject.uuid(), instanceId);
            }
        }

        final DMFInstance instance = new DMFInstance(resourceName, instanceId);

        final Matrix4dc transform = worldTransformToMatrix(object.get("Orientation"));
        instance.transform = new DMFTransform(transform);
        return instance;
    }

    @NotNull
    private DMFNode staticMeshInstanceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIReference resource = object.ref("Resource");
        final RTTIReference.FollowResult meshResource = Objects.requireNonNull(resource.follow(project, file));
        final RTTIObject meshResourceObject = meshResource.object();
        int instanceId = -1;

        final RTTIObject objectUUID = meshResourceObject.uuid();
        if (instances.containsKey(objectUUID)) {
            instanceId = instances.get(objectUUID);
        } else {
            final DMFNode instanceData;
            try (ProgressMonitor.Task task = monitor.begin("Exporting StaticMeshInstance Resource", 1)) {
                instanceData = toModel(task.split(1), meshResource.file(), meshResourceObject, nameFromReference(resource, resourceName));
            }
            if (instanceData != null) {
                instanceId = toInstanceSource(RTTIUtils.uuidToString(objectUUID), instanceData);
                instances.put(objectUUID, instanceId);
            }
        }

        final DMFInstance instance = new DMFInstance(resourceName, instanceId);
        final Matrix4dc transform = worldTransformToMatrix(object.get("Orientation"));
        instance.transform = new DMFTransform(transform);
        return instance;


    }

    @NotNull
    private DMFNode objectCollectionToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIReference[] objects = object.get("Objects");
        final DMFModelGroup group = new DMFModelGroup("Collection %s".formatted(resourceName));
        try (ProgressMonitor.Task task = monitor.begin("Exporting ObjectCollection Objects", objects.length)) {
            for (RTTIReference rttiReference : objects) {
                try {
                    final RTTIReference.FollowResult refObject = Objects.requireNonNull(rttiReference.follow(project, file));
                    final DMFNode node = toModel(task.split(1), refObject.file(), refObject.object(), nameFromReference(rttiReference, resourceName));
                    if (node != null) {
                        group.children.add(node);
                    }
                } catch (IOException e) {
                    log.warn("Failed to follow" + rttiReference);
                }
            }
        }
        return group;
    }

    @Nullable
    private DMFAttachment destructibilityPartToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference.FollowResult initialStateRef = object.ref("InitialState").follow(project, file);
        if (initialStateRef == null) {
            return null;
        }
        DMFNode state = toModel(monitor, initialStateRef.file(), initialStateRef.object(), initialStateRef.object().str("Name"));
        if (state == null) {
            return null;
        }
        DMFAttachment node = new DMFAttachment(object.str("Name"), object.str("BoneName"), new DMFTransform(mat44TransformToMatrix(object.obj("LocalMatrix"))));
        node.children.add(state);
        return node;
    }

    @Nullable
    private DMFNode destructibilityPartStateResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference.FollowResult modelPartResource = object.ref("ModelPartResource").follow(project, file);
        if (modelPartResource == null) {
            return null;
        }
        DMFNode state = toModel(monitor, modelPartResource.file(), modelPartResource.object(), modelPartResource.object().str("Name"));
        if (state == null) {
            return null;
        }
        final Matrix4dc offsetMatrix = mat44TransformToMatrix(object.obj("OffsetMatrix"));
        DMFNode node = new DMFModelGroup(resourceName);
        node.transform = new DMFTransform(offsetMatrix);
        node.children.add(state);
        return node;
    }

    @Nullable
    private DMFNode lodMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final RTTIObject[] meshes = object.get("Meshes");
        if (meshes.length == 0) {
            return null;
        }
        final DMFLodModel lodModel = new DMFLodModel(resourceName);
        try (ProgressMonitor.Task task = monitor.begin("Exporting lods", meshes.length)) {
            for (RTTIObject lodRef : meshes) {
                final RTTIReference meshRef = lodRef.ref("Mesh");
                final RTTIReference.FollowResult mesh = Objects.requireNonNull(meshRef.follow(project, file));
                final DMFNode lod = toModel(task.split(1), mesh.file(), mesh.object(), "%s_LOD%d".formatted(nameFromReference(meshRef, resourceName), 0));
                if (lod != null) {
                    if (!options.contains(ModelExporterProvider.Option.EXPORT_LODS)) {
                        return lod;
                    }
                    lodModel.addLod(lod, lodRef.f32("Distance"));
                }
            }
        }
        return lodModel;
    }

    @NotNull
    private DMFNode streamingTileResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final DMFModelGroup group = new DMFModelGroup(resourceName);
        final RTTIReference[] stateRefs = object.get("States");
        try (ProgressMonitor.Task task = monitor.begin("Exporting StreamingTileResource States", stateRefs.length)) {
            for (int i = 0; i < stateRefs.length; i++) {
                final RTTIReference ref = stateRefs[i];
                final RTTIReference.FollowResult stateRef = ref.follow(project, file);
                if (stateRef == null) {
                    continue;
                }
                final DMFLodModel lodModel = new DMFLodModel(resourceName);
                final RTTIReference[] lodRefs = stateRef.object().get("LODs");
                try (ProgressMonitor.Task lodTask = task.split(1).begin("Exporting StreamingTileResource lods", lodRefs.length)) {
                    for (RTTIReference lodRef : lodRefs) {
                        if (!lodModel.lods.isEmpty() && !options.contains(ModelExporterProvider.Option.EXPORT_LODS)) {
                            break;
                        }
                        final RTTIReference.FollowResult lodTileRef = lodRef.follow(project, stateRef.file());
                        if (lodTileRef == null) {
                            lodTask.worked(1);
                            continue;
                        }
                        log.debug("Extracting from %s".formatted(lodTileRef.file()));
                        RTTIReference objectCollection = lodTileRef.object().ref("ObjectCollection");
                        final RTTIReference.FollowResult objCollectionRef;
                        try {
                            objCollectionRef = objectCollection.follow(project, lodTileRef.file());
                        } catch (IOException e) {
                            log.warn("Failed to follow" + objectCollection);
                            continue;
                        }
                        if (objCollectionRef == null) {
                            lodTask.worked(1);
                            continue;
                        }

                        DMFNode node = toModel(lodTask.split(1), objCollectionRef.file(), objCollectionRef.object(), nameFromReference(ref, "%s_state_%d".formatted(resourceName, i)));
                        if (node != null) {
                            lodModel.addLod(node, 0);
                        }
                    }
                }
                group.children.add(lodModel);
            }
        }
        return group;
    }

    @NotNull
    private DMFNode multiMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final DMFModelGroup group = new DMFModelGroup(resourceName);
        if (project.getContainer().getType() == GameType.DSDC) {
            final RTTIReference[] meshes = object.refs("Meshes");
            final RTTIObject[] transforms = object.objs("Transforms");
            try (ProgressMonitor.Task task = monitor.begin("Processing meshes", meshes.length)) {
                for (int partId = 0; partId < meshes.length; partId++) {
                    final RTTIReference.FollowResult mesh = Objects.requireNonNull(meshes[partId].follow(project, file));
                    final DMFNode model = toModel(task.split(1), mesh.file(), mesh.object(), "%s_Part%d".formatted(nameFromReference(meshes[partId], resourceName), partId));
                    if (model == null) {
                        continue;
                    }
                    if (transforms.length > 0) {
                        if (model.transform != null) {
                            throw new IllegalStateException("Model already had transforms, please handle me!");
                        }
                        model.transform = new DMFTransform(mat34ToMatrix(transforms[partId]));
                    }
                    group.children.add(model);
                }
            }
        } else {
            final RTTIObject[] parts = object.get("Parts");

            try (ProgressMonitor.Task task = monitor.begin("Exporting MultiMeshResource Parts", parts.length)) {
                for (int partId = 0; partId < parts.length; partId++) {
                    final RTTIObject part = parts[partId];
                    final RTTIReference meshRef = part.ref("Mesh");
                    final RTTIReference.FollowResult mesh = Objects.requireNonNull(meshRef.follow(project, file));
                    final DMFNode model = toModel(task.split(1), mesh.file(), mesh.object(), "%s_Part%d".formatted(nameFromReference(meshRef, resourceName), partId));
                    if (model == null) {
                        continue;
                    }
                    if (model.transform != null) {
                        throw new IllegalStateException("Model already had transforms, please handle me!");
                    }
                    model.transform = new DMFTransform(worldTransformToMatrix(part.get("Transform")));
                    group.children.add(model);
                }
            }
        }
        return group;
    }

    @Nullable
    private DMFNode regularSkinnedMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTICoreFile file,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws IOException {
        final DrawFlags flags = DrawFlags.valueOf(object.obj("DrawFlags").i32("Data"), project.getTypeRegistry());
        if (!flags.renderType().equals("Normal")) {
            return null;
        }
        if (instances.containsKey(object.uuid()) && options.contains(ModelExporterProvider.Option.USE_INSTANCING)) {
            int instanceId = instances.get(object.uuid());
            instances.put(object.uuid(), instanceId);
            return new DMFInstance(resourceName, instanceId);
        }

        final DMFMesh mesh = new DMFMesh();
        final DMFModel model = new DMFModel(resourceName, mesh);
        if (object.type().getTypeName().equals("RegularSkinnedMeshResource")) {
            final DMFSkeleton currentSkeleton = Objects.requireNonNullElseGet(masterSkeleton, DMFSkeleton::new);

            final RTTIObject skeletonObj = Objects.requireNonNull(object.ref("Skeleton").get(project, file));

            final RTTIObject meshJointBindings = switch (project.getContainer().getType()) {
                case DS, DSDC -> Objects.requireNonNull(object.ref("SkinnedMeshJointBindings").get(project, file));
                case HZD -> Objects.requireNonNull(object.ref("SkinnedMeshBoneBindings").get(project, file));
            };
            validateSkeleton(currentSkeleton, skeletonObj);
            final RTTIObject[] joints = skeletonObj.get("Joints");
            final short[] jointIndexList = meshJointBindings.get("JointIndexList");
            final RTTIObject[] inverseBindMatrices = meshJointBindings.get("InverseBindMatrices");

            for (short i = 0; i < joints.length; i++) {
                int localBoneId = IOUtils.indexOf(jointIndexList, i);
                if (localBoneId == -1) {
                    continue;
                }
                DMFBone bone = currentSkeleton.findBone(joints[i].str("Name"));
                if (bone == null) {
                    throw new IllegalStateException("All new bones should've been created in validateSkeleton call");
                }
                bone.transform = new DMFTransform(mat44TransformToMatrix(inverseBindMatrices[localBoneId]).invert());
                bone.localSpace = false;
            }

            for (short targetId : jointIndexList) {
                RTTIObject targetBone = joints[targetId];
                mesh.boneRemapTable.put(targetId, (short) currentSkeleton.findBoneId(targetBone.str("Name")));
            }
            model.setSkeleton(currentSkeleton, scene);
        }
        switch (project.getContainer().getType()) {
            case DS, DSDC -> exportDSMeshData(monitor, file, object, mesh);
            case HZD -> exportHZDMeshData(monitor, file, object, mesh);
        }
        model.addToCollection(collectionStack.peek(), scene);
        if (options.contains(ModelExporterProvider.Option.USE_INSTANCING)) {
            int instanceId = toInstanceSource(RTTIUtils.uuidToString(object.uuid()), model);
            instances.put(object.uuid(), instanceId);
            return new DMFInstance(resourceName, instanceId);
        } else {
            return model;
        }
    }

    private void exportHZDMeshData(@NotNull ProgressMonitor monitor, @NotNull RTTICoreFile file, @NotNull RTTIObject object, DMFMesh mesh) throws IOException {

        final RTTIReference[] primitivesRefs = object.get("Primitives");
        final RTTIReference[] shadingGroupsRefs;
        if (object.type().getTypeName().equals("StaticMeshResource")) {
            shadingGroupsRefs = object.get("RenderEffects");
        } else {
            shadingGroupsRefs = object.get("RenderFxResources");
        }
        if (primitivesRefs.length != shadingGroupsRefs.length) {
            throw new IllegalStateException("Primitives count does not match ShadingGroups count!");
        }

        try (ProgressMonitor.Task exportTask = monitor.begin("Exporting primitives", primitivesRefs.length)) {
            for (int i = 0; i < primitivesRefs.length; i++) {
                final RTTIReference primitivesRef = primitivesRefs[i];
                final RTTIReference shadingGroupRef = shadingGroupsRefs[i];
                final RTTIReference.FollowResult primitiveRes = Objects.requireNonNull(primitivesRef.follow(project, file));
                final RTTIObject primitiveObj = primitiveRes.object();
                final RTTIObject shadingGroupObj = Objects.requireNonNull(shadingGroupRef.get(project, file));
                final RTTIObject vertexArrayObj = Objects.requireNonNull(primitiveObj.ref("VertexArray").get(project, primitiveRes.file()));
                final RTTIObject indexArrayObj = Objects.requireNonNull(primitiveObj.ref("IndexArray").get(project, primitiveRes.file()));
                final HZDVertexArrayResourceHandler.HwVertexArray vertices = vertexArrayObj.obj("Data").cast();
                final HZDIndexArrayResourceHandler.HwIndexArray indices = indexArrayObj.obj("Data").cast();

                final DMFPrimitive primitive = new DMFPrimitive(i, vertices.vertexCount, DMFVertexBufferType.SINGLE_BUFFER, 0, vertices.vertexCount,
                    indices.getIndexSize(), indices.indexCount, primitiveObj.i32("StartIndex"), primitiveObj.i32("EndIndex")
                );
                mesh.primitives.add(primitive);
                int vertexStreamOffset = -1;
                for (RTTIObject streamObj : vertices.streams) {
                    HZDVertexArrayResourceHandler.HwVertexStream stream = streamObj.cast();
                    final int stride = stream.stride;

                    final DMFBuffer buffer;
                    long resourceLength = 0;
                    if (stream.dataSource == null) {
                        buffer = new DMFInternalBuffer("INTERNAL", new DMFBuffer.ByteArrayDataProvider(stream.data));
                        scene.buffers.add(buffer);
                    } else {
                        final HZDDataSource dataSource = stream.dataSource.cast();
                        if (vertexStreamOffset == -1) {
                            vertexStreamOffset = dataSource.getOffset();
                        }

                        final String dataSourceLocation = dataSource.location.substring(dataSource.location.indexOf(":") + 1);
                        buffer = createDataBuffer(dataSourceLocation + "_" + RTTIUtils.uuidToString(streamObj.obj("Hash")),
                            dataSource, vertexStreamOffset, stride * vertices.vertexCount
                        );
                        resourceLength = dataSource.length;

                    }
                    vertexStreamOffset += resourceLength;
                    convertVertexAttributes(vertices.vertexCount, 0, primitive, stream, stride, buffer);

                }

                final DMFBuffer buffer;
                if (indices.dataSource == null) {
                    buffer = new DMFInternalBuffer("INTERNAL", new DMFBuffer.ByteArrayDataProvider(indices.data));
                    scene.buffers.add(buffer);
                } else {
                    final HZDDataSource dataSource = indices.dataSource.cast();
                    final String dataSourceLocation = dataSource.location.substring(dataSource.location.indexOf(":") + 1);
                    buffer = createDataBuffer(dataSourceLocation + "_" + RTTIUtils.uuidToString(indices.hash), dataSource, dataSource.getOffset(), indices.getIndexSize() * indices.indexCount);
                }
                final DMFBufferView bufferView = new DMFBufferView(scene.buffers.indexOf(buffer), 0, indices.getIndexSize() * indices.indexCount);
                primitive.setIndexBufferView(bufferView, scene);

                final RTTIObject materialUUID = shadingGroupObj.uuid();
                final String materialName = RTTIUtils.uuidToString(materialUUID);
                DMFMaterial material = scene.getMaterial(materialName);
                if (material == null) {
                    material = scene.createMaterial(materialName);
                    exportMaterial(exportTask.split(1), shadingGroupObj, material, file);
                } else {
                    exportTask.worked(1);
                }
                primitive.setMaterial(material, scene);
            }
        }
    }

    private void exportDSMeshData(@NotNull ProgressMonitor monitor, @NotNull RTTICoreFile file, @NotNull RTTIObject object, DMFMesh mesh) throws IOException {
        final DSDataSource dataSource = object.obj("DataSource").cast();
        final String dataSourceLocation = "%s.file.stream".formatted(dataSource.location);
        final DMFBuffer buffer = createDataBuffer(dataSourceLocation, dataSource, 0, dataSource.length);

        final Map<RTTIObject, Map.Entry<Integer, Integer>> bufferOffsets = new HashMap<>();

        final RTTIReference[] primitivesRefs = object.get("Primitives");
        final RTTIReference[] shadingGroupsRefs = object.get("ShadingGroups");
        if (primitivesRefs.length != shadingGroupsRefs.length) {
            throw new IllegalStateException("Primitives count does not match ShadingGroups count!");
        }
        int dataSourceOffset = 0;
        for (RTTIReference primitivesRef : primitivesRefs) {
            final RTTIReference.FollowResult primitiveRes = Objects.requireNonNull(primitivesRef.follow(project, file));
            final RTTIObject primitiveObj = primitiveRes.object();
            final RTTIObject vertexArrayObj = Objects.requireNonNull(primitiveObj.ref("VertexArray").get(project, primitiveRes.file()));
            final RTTIObject indexArrayObj = Objects.requireNonNull(primitiveObj.ref("IndexArray").get(project, primitiveRes.file()));
            final DSVertexArrayResourceHandler.HwVertexArray vertices = vertexArrayObj.obj("Data").cast();
            final DSIndexArrayResourceHandler.HwIndexArray indices = indexArrayObj.obj("Data").cast();

            final RTTIObject vertexArrayUUID = vertexArrayObj.uuid();
            if (!bufferOffsets.containsKey(vertexArrayUUID)) {
                bufferOffsets.put(vertexArrayUUID, Map.entry(dataSourceOffset, bufferOffsets.size()));
                for (RTTIObject stream : vertices.streams) {
                    final int stride = stream.i32("Stride");
                    dataSourceOffset += MathUtils.alignUp(stride * vertices.vertexCount, 256);
                }
            }
            final RTTIObject indicesArrayUUID = indexArrayObj.uuid();
            if (!bufferOffsets.containsKey(indicesArrayUUID)) {
                bufferOffsets.put(indicesArrayUUID, Map.entry(dataSourceOffset, bufferOffsets.size()));
                dataSourceOffset += MathUtils.alignUp(indices.getIndexSize() * indices.indexCount, 256);
            }
        }
        try (ProgressMonitor.Task exportTask = monitor.begin("Exporting primitives", primitivesRefs.length)) {
            for (int i = 0; i < primitivesRefs.length; i++) {
                RTTIReference primitivesRef = primitivesRefs[i];
                RTTIReference shadingGroupRef = shadingGroupsRefs[i];
                final RTTIReference.FollowResult primitiveRes = Objects.requireNonNull(primitivesRef.follow(project, file));
                final RTTIObject primitiveObj = primitiveRes.object();
                final RTTIObject shadingGroupObj = Objects.requireNonNull(shadingGroupRef.follow(project, file)).object();
                final RTTIObject vertexArrayObj = Objects.requireNonNull(primitiveObj.ref("VertexArray").get(project, primitiveRes.file()));
                final RTTIObject indexArrayObj = Objects.requireNonNull(primitiveObj.ref("IndexArray").get(project, primitiveRes.file()));
                final DSVertexArrayResourceHandler.HwVertexArray vertices = vertexArrayObj.obj("Data").cast();
                final DSIndexArrayResourceHandler.HwIndexArray indices = indexArrayObj.obj("Data").cast();

                Map.Entry<Integer, Integer> offsetAndGroupId = bufferOffsets.get(vertexArrayObj.uuid());
                dataSourceOffset = offsetAndGroupId.getKey();

                final DMFPrimitive primitive = new DMFPrimitive(offsetAndGroupId.getValue(), vertices.vertexCount, DMFVertexBufferType.SINGLE_BUFFER, 0, vertices.vertexCount,
                    indices.getIndexSize(), indices.indexCount, primitiveObj.i32("StartIndex"), primitiveObj.i32("EndIndex")
                );
                mesh.primitives.add(primitive);

                for (RTTIObject streamObj : vertices.streams) {
                    DSVertexArrayResourceHandler.HwVertexStream stream = streamObj.cast();
                    convertVertexAttributes(vertices.vertexCount, dataSourceOffset, primitive, stream, stream.stride, buffer);
                    dataSourceOffset += MathUtils.alignUp(stream.stride * vertices.vertexCount, 256);
                }

                offsetAndGroupId = bufferOffsets.get(indexArrayObj.uuid());
                final DMFBufferView bufferView = new DMFBufferView(scene.buffers.indexOf(buffer), offsetAndGroupId.getKey(), indices.getIndexSize() * indices.indexCount);

                primitive.setIndexBufferView(bufferView, scene);

                final RTTIObject materialUUID = shadingGroupObj.uuid();
                final String materialName = RTTIUtils.uuidToString(materialUUID);
                DMFMaterial material = scene.getMaterial(materialName);
                if (material == null) {
                    material = scene.createMaterial(materialName);
                    exportMaterial(exportTask.split(1), shadingGroupObj, material, file);
                } else {
                    exportTask.worked(1);

                }
                primitive.setMaterial(material, scene);

            }
        }
    }

    private void exportMaterial(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTIObject shadingGroup,
        @NotNull DMFMaterial material,
        @NotNull RTTICoreFile file
    ) throws IOException {
        final RTTIObject renderEffect;
        switch (project.getContainer().getType()) {
            case DS, DSDC -> renderEffect = shadingGroup.ref("RenderEffect").get(project, file);
            case HZD -> renderEffect = shadingGroup;
            default -> throw new IllegalStateException();
        }
        if (renderEffect == null) {
            return;
        }

        material.type = renderEffect.str("EffectType");
        if (!options.contains(ModelExporterProvider.Option.EXPORT_TEXTURES)) {
            return;
        }
        final RTTIObject[] techniqueSets = renderEffect.objs("TechniqueSets");
        try (ProgressMonitor.Task techniqueSetsExportTask = monitor.begin("Exporting TechniqueSets", techniqueSets.length)) {
            for (RTTIObject techniqueSet : techniqueSets) {
                exportTechniqueSet(techniqueSetsExportTask, techniqueSet, material, file);
            }
        }
    }

    private void exportTechniqueSet(ProgressMonitor.Task techniqueSetsExportTask, RTTIObject techniqueSet, @NotNull DMFMaterial material, @NotNull RTTICoreFile file) throws IOException {
        final RTTIObject[] renderTechniques = techniqueSet.objs("RenderTechniques");
        try (ProgressMonitor.Task renderTechniquesExportTask = techniqueSetsExportTask.split(1).begin("Exporting RenderTechniques", renderTechniques.length)) {
            for (RTTIObject renderTechnique : renderTechniques) {
                final String techniqueType = renderTechnique.str("TechniqueType");
                if (!(techniqueType.equals("Deferred") || techniqueType.equals("CustomDeferred") || techniqueType.equals("DeferredEmissive"))) {
                    log.warn("Skipped %s".formatted(techniqueType));
                    continue;
                }

                final RTTIObject[] textureBindings = renderTechnique.get("TextureBindings");
                try (ProgressMonitor.Task textureBindingsExportTask = renderTechniquesExportTask.split(1).begin("Exporting textureBindings", textureBindings.length)) {
                    for (RTTIObject textureBinding : textureBindings) {
                        exportTextureBinding(textureBindingsExportTask.split(1), file, material, textureBinding);
                    }
                }
            }
        }
    }

    private void exportTextureBinding(ProgressMonitor monitor, @NotNull RTTICoreFile file, @NotNull DMFMaterial material, RTTIObject textureBinding) throws IOException {
        final RTTITypeEnum textureSetTypeEnum = project.getTypeRegistry().find("ETextureSetType");
        final RTTIReference textureRef = textureBinding.ref("TextureResource");
        final RTTIReference.FollowResult textureRes = textureRef.follow(project, file);
        if (textureRes == null) {
            return;
        }
        int packedData = textureBinding.i32("PackedData");
        int usageType = packedData >> 2 & 15;
        String textureUsageName = textureSetTypeEnum.valueOf(usageType).name();
        if (textureUsageName.equals("Invalid")) {
            textureUsageName = nameFromReference(textureRef, "Texture_%s".formatted(uuidToString(textureRef)));
        }

        final RTTIObject textureObj = textureRes.object();
        if (textureObj.type().getTypeName().equals("Texture")) {
            final String textureName = nameFromReference(textureRef, RTTIUtils.uuidToString(textureObj.uuid()));
            log.debug("Extracting \"{}\" texture", textureName);

            if (scene.getTexture(textureName) != null) {
                int textureId2 = scene.textures.indexOf(scene.getTexture(textureName));
                if (!material.textureIds.containsValue(textureId2)) {
                    material.textureIds.put(textureUsageName, textureId2);
                }
                return;

            }
            DMFTexture dmfTexture;
            try (ProgressMonitor.Task textureExportTask = monitor.begin("Exporting Texture", 1)) {
                dmfTexture = exportTexture(textureExportTask.split(1), textureObj, textureName);
            }
            if (dmfTexture == null) {
                dmfTexture = DMFTexture.nonExportableTexture(textureName);
            }
            dmfTexture.usageType = textureUsageName;
            material.textureIds.put(textureUsageName, scene.textures.indexOf(dmfTexture));

        } else if (textureObj.type().getTypeName().equals("TextureSet")) {
            final RTTIObject[] entries = textureObj.get("Entries");

            String channels = "";
            int textureId = -1;
            try (ProgressMonitor.Task textureExportTask = monitor.begin("Exporting TextureSet", entries.length)) {
                for (int i = 0; i < entries.length; i++) {
                    RTTIObject entry = entries[i];
                    int usageInfo = entry.i32("PackingInfo");
                    final String tmp = PackingInfoHandler.getInfo(usageInfo & 0xFF) +
                        PackingInfoHandler.getInfo(usageInfo >>> 8 & 0xff) +
                        PackingInfoHandler.getInfo(usageInfo >>> 16 & 0xff) +
                        PackingInfoHandler.getInfo(usageInfo >>> 24 & 0xff);
                    if (tmp.contains(textureUsageName)) {
                        final RTTIReference textureSetTextureRef = entry.ref("Texture");
                        final RTTIObject textureSetTexture = textureSetTextureRef.get(project, textureRes.file());
                        if (textureSetTexture == null) {
                            continue;
                        }
                        final String textureName = nameFromReference(textureRef, "Texture_%s".formatted(uuidToString(textureSetTextureRef))) + "_%d".formatted(i);
                        DMFTexture texture = exportTexture(textureExportTask.split(1), textureSetTexture, textureName);
                        textureId = scene.textures.indexOf(texture);
                        if (PackingInfoHandler.getInfo(usageInfo & 0xFF).contains(textureUsageName)) {
                            channels += "R";
                        }
                        if (PackingInfoHandler.getInfo(usageInfo >>> 8 & 0xff).contains(textureUsageName)) {
                            channels += "G";
                        }
                        if (PackingInfoHandler.getInfo(usageInfo >>> 16 & 0xff).contains(textureUsageName)) {
                            channels += "B";
                        }
                        if (PackingInfoHandler.getInfo(usageInfo >>> 24 & 0xff).contains(textureUsageName)) {
                            channels += "A";
                        }
                        break;
                    }
                }
            }
            final DMFTextureDescriptor descriptor = new DMFTextureDescriptor(textureId, channels, textureUsageName);

            material.textureDescriptors.add(descriptor);
        } else {
            log.warn("Texture of type {} not supported", textureObj.type().getTypeName());
        }
    }

    @Nullable
    private DMFTexture exportTexture(ProgressMonitor monitor, @NotNull RTTIObject texture, @NotNull String textureName) throws IOException {
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
        final ImageProvider imageProvider = TextureViewer.getImageProvider(texture, project.getPackfileManager());
        if (imageProvider == null) {
            return null;
        }
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final String ext;
        if (imageProvider.getImageReader().getColorModel().getPixelSize() > 32) {
            new TextureExporterTIFF().export(monitor, imageProvider, Set.of(), Channels.newChannel(stream));
            ext = ".tiff";
        } else {
            new TextureExporterPNG().export(monitor, imageProvider, Set.of(), Channels.newChannel(stream));
            ext = ".png";
        }
        final byte[] src = stream.toByteArray();
        final DMFTexture dmfTexture;
        final DMFBuffer buffer;

        if (options.contains(ModelExporterProvider.Option.EMBED_TEXTURES)) {
            // fixme
            buffer = new DMFInternalBuffer(textureName, new DMFBuffer.ByteArrayDataProvider(src));
        } else {
            Files.write(getBuffersPath().resolve(textureName + ext), src);
            buffer = new DMFExternalBuffer(textureName, textureName + ext, new DMFBuffer.ByteArrayDataProvider(src));

        }
        scene.buffers.add(buffer);
        dmfTexture = new DMFTexture(textureName, scene.buffers.indexOf(buffer));
        scene.textures.add(dmfTexture);

        return dmfTexture;
    }

    private DMFSkeleton exportSkeleton(@NotNull RTTIObject skeleton) {
        final DMFSkeleton dmfSkeleton = new DMFSkeleton();
        RTTIObject[] bones = skeleton.objs("Joints");
        for (final RTTIObject bone : bones) {
            dmfSkeleton.newBone(bone.str("Name"), DMFTransform.IDENTITY, bone.i16("ParentIndex"));
        }
        return dmfSkeleton;
    }

    private void validateSkeleton(@NotNull DMFSkeleton currentSkeleton, @NotNull RTTIObject newSkeleton) {
        RTTIObject[] bones = newSkeleton.objs("Joints");
        for (final RTTIObject bone : bones) {
            final String boneName = bone.str("Name");
            if (currentSkeleton.findBone(boneName) == null) {
                currentSkeleton.newBone(boneName, DMFTransform.IDENTITY, bone.i16("ParentIndex"));
            }
        }
    }

    private void exportHelpers(@NotNull RTTIObject helper, @NotNull DMFSkeleton masterSkeleton, @NotNull RTTIObject skeleton) {
        RTTIObject[] bones = skeleton.objs("Joints");
        for (RTTIObject helperNode : helper.objs("Helpers")) {
            final int boneId = helperNode.i32("Index");
            if (boneId == -1) {
                masterSkeleton.newBone(
                    helperNode.str("Name"),
                    new DMFTransform(mat44TransformToMatrix(helperNode.obj("Matrix"))),
                    -1
                ).localSpace = false;
            } else {
                String boneName = bones[boneId].str("Name");
                masterSkeleton.newBone(
                    helperNode.str("Name"),
                    new DMFTransform(mat44TransformToMatrix(helperNode.obj("Matrix"))),
                    masterSkeleton.findBoneId(boneName)
                ).localSpace = true;
            }
        }
    }

    @NotNull
    private DMFBuffer createDataBuffer(@NotNull String name, @NotNull HwDataSource dataSource, int offset, int length) {
        return createDataBuffer(name, new DMFBuffer.DataSourceDataProvider(project, dataSource, offset, length));
    }

    @NotNull
    private DMFBuffer createDataBuffer(@NotNull String name, @NotNull DMFBuffer.DataProvider provider) {
        DMFBuffer buffer;
        if (options.contains(ModelExporterProvider.Option.EMBED_BUFFERS)) {
            buffer = new DMFInternalBuffer(name, provider);
        } else {
            buffer = new DMFExternalBuffer(name, name + ".dbuf", provider);
        }
        scene.buffers.add(buffer);
        return buffer;
    }

    @NotNull
    public Path getBuffersPath() throws IOException {
        final Path buffersPath = output.resolve("dbuffers");
        Files.createDirectories(buffersPath);
        return buffersPath;
    }

    @NotNull
    private JsonWriter createJsonWriter(@NotNull Writer writer) throws IOException {
        final JsonWriter jsonWriter = gson.newJsonWriter(writer);
        jsonWriter.setHtmlSafe(false);
        jsonWriter.setLenient(false);
        jsonWriter.setIndent("\t");
        return jsonWriter;
    }

}
