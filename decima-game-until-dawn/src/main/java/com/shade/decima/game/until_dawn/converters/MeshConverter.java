package com.shade.decima.game.until_dawn.converters;

import com.shade.decima.game.Converter;
import com.shade.decima.game.until_dawn.game.UntilDawnGame;
import com.shade.decima.game.until_dawn.rtti.UntilDawn.*;
import com.shade.decima.geometry.*;
import com.shade.decima.math.Mat4;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MeshConverter implements Converter<UntilDawnGame, Node> {
    private static final Logger log = LoggerFactory.getLogger(MeshConverter.class);

    @Override
    public boolean supports(Object object) {
        return object instanceof StaticMeshResource
            || object instanceof MultiMeshResource
            || object instanceof RegularSkinnedMeshResource
            || object instanceof LodMeshResource;
    }

    @Override
    public Optional<Node> convert(Object object, UntilDawnGame game) {
        return convertResource((MeshResourceBase) object);
    }

    private Optional<Node> convertResource(MeshResourceBase resource) {
        return switch (resource) {
            case StaticMeshResource r -> convertStaticMeshResource(r);
            case MultiMeshResource r -> convertMultiMeshResource(r);
            case RegularSkinnedMeshResource r -> convertRegularSkinnedMeshResource(r);
            case LodMeshResource r -> convertLodMeshResource(r);
            default -> {
                log.error("Unsupported resource type: {}", resource);
                yield Optional.empty();
            }
        };
    }

    private Optional<Node> convertStaticMeshResource(StaticMeshResource resource) {
        var primitives = resource.meshDescription().primitives().stream()
            .map(Ref::get)
            .map(MeshConverter::convertPrimitive)
            .toList();

        var mesh = new Mesh(resource.meshName(), primitives);
        var lts = resource.meshDescription().localTranslateScale();
        var matrix = Mat4.identity().translate(lts.x(), lts.y(), lts.z()).scale(lts.w());
        var node = new Node(resource.general().name(), mesh, List.of(), matrix);

        return Optional.of(node);
    }

    private Optional<Node> convertMultiMeshResource(MultiMeshResource resource) {
        var children = resource.mesh().parts().stream()
            .map(this::convertMultiMeshResourcePart)
            .flatMap(Optional::stream)
            .toList();

        var node = new Node(resource.general().name(), null, children, Mat4.identity());

        return Optional.of(node);
    }

    private Optional<Node> convertMultiMeshResourcePart(MultiMeshResourcePart part) {
        var child = convertResource(part.mesh().get());
        var matrix = convertMat4(part.transform());

        return child.map(c -> c.transform(matrix));
    }

    private Optional<Node> convertRegularSkinnedMeshResource(RegularSkinnedMeshResource resource) {
        var primitives = resource.primitives().stream()
            .map(Ref::get)
            .map(MeshConverter::convertPrimitive)
            .toList();

        var mesh = new Mesh(resource.meshName(), primitives);
        var node = new Node(resource.general().name(), mesh, List.of(), Mat4.identity());

        return Optional.of(node);
    }

    private Optional<Node> convertLodMeshResource(LodMeshResource resource) {
        var part = resource.mesh().meshes().getFirst();
        return convertResource(part.mesh().get());
    }

    private static Primitive convertPrimitive(PrimitiveResource primitiveResource) {
        var vertexArray = primitiveResource.vertexArray().get();
        var indexArray = primitiveResource.indexArray().get();

        var vertexAccessors = buildVertexAccessors(vertexArray);
        var indexAccessor = buildIndexAccessor(indexArray);

        return new Primitive(indexAccessor, vertexAccessors);
    }

    private static Mat4 convertMat4(Mat44 mat) {
        return new Mat4(
            mat.col0().x(), mat.col0().y(), mat.col0().z(), mat.col0().w(),
            mat.col1().x(), mat.col1().y(), mat.col1().z(), mat.col1().w(),
            mat.col2().x(), mat.col2().y(), mat.col2().z(), mat.col2().w(),
            mat.col3().x(), mat.col3().y(), mat.col3().z(), mat.col3().w()
        );
    }

    private static Map<Semantic, Accessor> buildVertexAccessors(VertexArrayResource vertexArray) {
        var accessors = new HashMap<Semantic, Accessor>();
        var count = vertexArray.count();

        for (var stream : vertexArray.streams()) {
            var buffer = ByteBuffer.wrap(stream.data()).order(ByteOrder.LITTLE_ENDIAN);
            var stride = stream.stride();

            for (var element : stream.elements()) {
                var offset = Byte.toUnsignedInt(element.offset());

                var semantic = switch (element.type()) {
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
                        log.warn("Skipping unsupported element attribute: {}", element.type());
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
                        log.warn("Skipping unsupported element size: {}", element.slotsUsed());
                        yield null;
                    }
                };

                if (elementType == null) {
                    continue;
                }

                var accessor = switch (element.storageType()) {
                    // @formatter:off
                    case UnsignedByte ->
                        new Accessor(buffer, elementType, ComponentType.UNSIGNED_BYTE, offset, count, stride, false);
                    case UnsignedByteNormalized ->
                        new Accessor(buffer, elementType, ComponentType.UNSIGNED_BYTE, offset, count, stride, true);
                    case SignedShortNormalized ->
                        new Accessor(buffer, elementType, ComponentType.SHORT, offset, count, stride, true);
                    case SignedShort ->
                        new Accessor(buffer, elementType, ComponentType.SHORT, offset, count, stride, false);
                    case UnsignedShort ->
                        new Accessor(buffer, elementType, ComponentType.UNSIGNED_SHORT, offset, count, stride, false);
                    case UnsignedShortNormalized ->
                        new Accessor(buffer, elementType, ComponentType.UNSIGNED_SHORT, offset, count, stride, true);
                    case Float ->
                        new Accessor(buffer, elementType, ComponentType.FLOAT, offset, count, stride, false);
                    case HalfFloat ->
                        new Accessor(buffer, elementType, ComponentType.HALF_FLOAT, offset, count, stride, false);
                    case X10Y10Z10W2Normalized ->
                        new Accessor(buffer, elementType, ComponentType.INT_10_10_10_2, offset, count, stride, true);
                    default -> {
                        log.warn("Skipping unsupported element type: {}", element.storageType());
                        yield null;
                    }
                    // @formatter:on
                };

                if (accessor != null) {
                    accessors.put(semantic, accessor);
                }
            }
        }

        return Map.copyOf(accessors);
    }

    private static Accessor buildIndexAccessor(IndexArrayResource indexArray) {
        var buffer = ByteBuffer.wrap(indexArray.data()).order(ByteOrder.LITTLE_ENDIAN);
        var component = switch (indexArray.format()) {
            case EIndexFormat.Index16 -> ComponentType.UNSIGNED_SHORT;
            case EIndexFormat.Index32 -> ComponentType.UNSIGNED_INT;
        };
        return new Accessor(buffer, ElementType.SCALAR, component, 0, indexArray.count());
    }
}
