package com.shade.decima.game.hfw.converters;

import com.shade.decima.game.Converter;
import com.shade.decima.game.hfw.game.ForbiddenWestGame;
import com.shade.decima.geometry.*;
import com.shade.decima.math.Mat4;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.scene.Node;
import com.shade.util.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;

import static com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.*;

public final class MeshToNodeConverter implements Converter<ForbiddenWestGame, Node> {
    private static final Logger log = LoggerFactory.getLogger(MeshToNodeConverter.class);

    @Override
    public boolean supports(Object object) {
        return object instanceof StaticMeshResource
            || object instanceof RegularSkinnedMeshResource
            || object instanceof LodMeshResource
            || object instanceof MultiMeshResource;
    }

    @Override
    public Optional<Node> convert(Object object, ForbiddenWestGame game) {
        return convertResource((MeshResourceBase) object, game);
    }

    private Optional<Node> convertResource(MeshResourceBase resource, ForbiddenWestGame game) {
        return switch (resource) {
            case StaticMeshResource r -> convertStaticMeshResource(r, game);
            case RegularSkinnedMeshResource r -> convertRegularSkinnedMeshResource(r, game);
            case LodMeshResource r -> convertLodMeshResource(r, game);
            case MultiMeshResource r -> convertMultiMeshResource(r, game);
            default -> {
                log.error("Unsupported resource type: {}", resource);
                yield Optional.empty();
            }
        };
    }

    private Optional<Node> convertStaticMeshResource(StaticMeshResource resource, ForbiddenWestGame game) {
        return convertMesh(
            resource.meshDescription().shadingGroups(),
            resource.meshDescription().primitives(),
            resource.meshDescription().streamingDataSource(),
            game
        );
    }

    private Optional<Node> convertRegularSkinnedMeshResource(RegularSkinnedMeshResource resource, ForbiddenWestGame game) {
        return convertMesh(
            resource.shadingGroups(),
            resource.primitives(),
            resource.streamingDataSource(),
            game
        );
    }

    private Optional<Node> convertLodMeshResource(LodMeshResource resource, ForbiddenWestGame game) {
        var part = resource.runtimeMeshes().getFirst();
        return convertResource(part.mesh().get(), game);
    }

    private Optional<Node> convertMultiMeshResource(MultiMeshResource resource, ForbiddenWestGame game) {
        var meshes = resource.meshes();
        var transforms = resource.transforms();
        var children = IntStream.range(0, meshes.size())
            .mapToObj(i -> convertMultiMeshResourcePart(meshes.get(i).get(), transforms.isEmpty() ? null : transforms.get(i), game))
            .flatMap(Optional::stream)
            .toList();

        var node = new Node(null, null, children, Mat4.identity());

        return Optional.of(node);
    }

    private Optional<Node> convertMultiMeshResourcePart(MeshResourceBase resource, Mat34 transform, ForbiddenWestGame game) {
        var child = convertResource(resource, game);
        var matrix = transform != null ? convertMat34(transform) : Mat4.identity();

        return child.map(c -> c.transform(matrix));
    }

    private Mat4 convertMat34(Mat34 matrix) {
        return new Mat4(
            matrix.row0().x(), matrix.row1().x(), matrix.row2().x(), 0.f,
            matrix.row0().y(), matrix.row1().y(), matrix.row2().y(), 0.f,
            matrix.row0().z(), matrix.row1().z(), matrix.row2().z(), 0.f,
            matrix.row0().w(), matrix.row1().w(), matrix.row2().w(), 1.f
        );
    }

    private Optional<Node> convertMesh(
        List<Ref<ShadingGroup>> shadingGroups,
        List<Ref<PrimitiveResource>> primitiveResources,
        StreamingDataSource dataSource,
        ForbiddenWestGame game
    ) {
        var buffer = readDataSource(dataSource, game);
        var primitives = new ArrayList<Primitive>();

        assert shadingGroups.size() == primitiveResources.size();

        for (int i = 0; i < shadingGroups.size(); i++) {
            var primitive = primitiveResources.get(i).get();

            if (primitive.startIndex() > 0) {
                throw new NotImplementedException();
            }

            var vertexArray = primitive.vertexArray().get();
            var vertexAccessors = buildVertexAccessors(vertexArray, buffer);

            var indexArray = primitive.indexArray().get();
            var indexAccessor = buildIndexAccessor(indexArray, buffer, primitive.startIndex(), primitive.endIndex());

            primitives.add(new Primitive(indexAccessor, vertexAccessors));
        }

        if (buffer != null && buffer.hasRemaining()) {
            throw new IllegalStateException("Not all data was read from the buffer");
        }

        var mesh = new Mesh(null, primitives);
        var node = new Node(null, mesh, List.of(), Mat4.identity());

        return Optional.of(node);
    }

    private static Map<Semantic, Accessor> buildVertexAccessors(VertexArrayResource vertexArray, ByteBuffer buffer) {
        var accessors = new HashMap<Semantic, Accessor>();
        var count = vertexArray.count();

        for (var stream : vertexArray.streams()) {
            var stride = stream.stride();

            ByteBuffer view;
            if (vertexArray.streaming()) {
                view = readDataAligned(buffer, count, stride);
            } else {
                view = ByteBuffer.wrap(stream.data());
            }

            for (var element : stream.elements()) {
                var offset = Byte.toUnsignedInt(element.offset());

                var semantic = switch (element.element()) {
                    case Pos -> Semantic.POSITION;
                    case Tangent -> Semantic.TANGENT;
                    case Normal -> Semantic.NORMAL;
                    case Color -> Semantic.COLOR_0;
                    case UV0 -> Semantic.TEXTURE_0;
                    case UV1 -> Semantic.TEXTURE_1;
                    case BlendWeights -> Semantic.WEIGHTS_0;
                    case BlendWeights2 -> Semantic.WEIGHTS_1;
                    case BlendIndices -> Semantic.JOINTS_0;
                    case BlendIndices2 -> Semantic.JOINTS_1;
                    default -> {
                        log.warn("Skipping unsupported element (semantic: {})", element.element());
                        yield null;
                    }
                };

                if (semantic == null) {
                    continue;
                }

                var elementType = switch (element.slotsUsed()) {
                    case 1 -> ElementType.SCALAR;
                    case 2 -> ElementType.VEC2;
                    case 3 -> ElementType.VEC3;
                    case 4 -> ElementType.VEC4;
                    default -> {
                        log.warn("Skipping unsupported element (semantic: {}, size: {})", element.element(), element.slotsUsed());
                        yield null;
                    }
                };

                if (elementType == null) {
                    continue;
                }

                var accessor = switch (element.storageType()) {
                    // @formatter:off
                    case UnsignedByte ->
                        new Accessor(view, elementType, ComponentType.UNSIGNED_BYTE, offset, count, stride, false);
                    case UnsignedByteNormalized ->
                        new Accessor(view, elementType, ComponentType.UNSIGNED_BYTE, offset, count, stride, true);
                    case UnsignedShort ->
                        new Accessor(view, elementType, ComponentType.UNSIGNED_SHORT, offset, count, stride, false);
                    case UnsignedShortNormalized ->
                        new Accessor(view, elementType, ComponentType.UNSIGNED_SHORT, offset, count, stride, true);
                    case SignedShort ->
                        new Accessor(view, elementType, ComponentType.SHORT, offset, count, stride, false);
                    case SignedShortNormalized ->
                        new Accessor(view, elementType, ComponentType.SHORT, offset, count, stride, true);
                    case HalfFloat ->
                        new Accessor(view, elementType, ComponentType.HALF_FLOAT, offset, count, stride, false);
                    case Float ->
                        new Accessor(view, elementType, ComponentType.FLOAT, offset, count, stride, false);
                    default -> {
                        log.warn("Skipping unsupported element (semantic: {}, format: {})", element.element(), element.storageType());
                        yield null;
                    }
                    // @formatter:on
                };

                if (accessor != null) {
                    accessors.put(semantic, accessor);
                }
            }
        }

        return accessors;
    }

    private static Accessor buildIndexAccessor(IndexArrayResource indexArray, ByteBuffer buffer, int startIndex, int endIndex) {
        var component = switch (indexArray.format()) {
            case Index16 -> ComponentType.UNSIGNED_SHORT;
            case Index32 -> ComponentType.UNSIGNED_INT;
        };

        var count = indexArray.count();
        var stride = indexArray.format().stride();

        ByteBuffer view;
        if (indexArray.streaming()) {
            view = readDataAligned(buffer, count, stride);
        } else {
            view = ByteBuffer.wrap(indexArray.data());
        }

        return new Accessor(view, ElementType.SCALAR, component, startIndex * stride, endIndex - startIndex, stride);
    }

    private static ByteBuffer readDataAligned(ByteBuffer buffer, int count, int stride) {
        int position = alignUp(buffer.position(), stride);
        int size = count * stride;
        var view = buffer.slice(position, size);
        buffer.position(position + size);
        return view;
    }

    private static ByteBuffer readDataSource(StreamingDataSource dataSource, ForbiddenWestGame game) {
        if (dataSource.length() == 0) {
            return null;
        }
        try {
            return ByteBuffer.wrap(game.getStreamingSystem().getDataSourceData(dataSource));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int alignUp(int value, int alignment) {
        return Math.ceilDiv(value, alignment) * alignment;
    }
}
