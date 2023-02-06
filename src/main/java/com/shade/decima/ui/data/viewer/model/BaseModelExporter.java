package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.decima.ui.data.viewer.model.utils.Matrix3x3;
import com.shade.decima.ui.data.viewer.model.utils.Matrix4x4;
import com.shade.decima.ui.data.viewer.model.utils.Transform;
import com.shade.decima.ui.data.viewer.model.utils.Vector3;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.util.UUID;

public class BaseModelExporter {
    @NotNull
    protected static Transform worldTransformToTransform(@NotNull RTTIObject transform) {
        final var pos = transform.obj("Position");
        final var ori = transform.obj("Orientation");
        final var col0 = ori.obj("Col0");
        final var col1 = ori.obj("Col1");
        final var col2 = ori.obj("Col2");

        final Vector3 translation = new Vector3(new double[]{
            pos.f64("X"),
            pos.f64("Y"),
            pos.f64("Z")
        });

        final Matrix3x3 rotationAndScale = new Matrix3x3(new double[][]{
            new double[]{col0.f32("X"), col0.f32("Y"), col0.f32("Z")},
            new double[]{col1.f32("X"), col1.f32("Y"), col1.f32("Z")},
            new double[]{col2.f32("X"), col2.f32("Y"), col2.f32("Z")}
        });

        return new Transform(translation, rotationAndScale.toQuaternion(), rotationAndScale.toScale());
    }

    @NotNull
    static Matrix4x4 mat44TransformToMatrix4x4(@NotNull RTTIObject transform) {
        final var col0 = transform.obj("Col0");
        final var col1 = transform.obj("Col1");
        final var col2 = transform.obj("Col2");
        final var col3 = transform.obj("Col3");

        return new Matrix4x4(new double[][]{
            new double[]{col0.f32("X"), col1.f32("X"), col2.f32("X"), col3.f32("X")},
            new double[]{col0.f32("Y"), col1.f32("Y"), col2.f32("Y"), col3.f32("Y")},
            new double[]{col0.f32("Z"), col1.f32("Z"), col2.f32("Z"), col3.f32("Z")},
            new double[]{col0.f32("W"), col1.f32("W"), col2.f32("W"), col3.f32("W")}
        });
    }

    @NotNull
    protected static String nameFromReference(@NotNull RTTIReference reference, @NotNull String name) {
        if (reference instanceof RTTIReference.External ref) {
            String path = ref.path();
            return path.substring(path.lastIndexOf("/") + 1);
        } else {
            return name;
        }
    }

    @NotNull
    protected static String uuidToString(@NotNull RTTIObject uuid) {
        return GGUUIDValueHandler.INSTANCE.getString(uuid.type(), uuid);
    }

    @NotNull
    protected static String uuidToString(@NotNull RTTIReference uuid) {
        if (uuid instanceof RTTIReference.External ref) {
            return GGUUIDValueHandler.toString(ref.uuid());
        } else if (uuid instanceof RTTIReference.Internal ref) {
            return GGUUIDValueHandler.toString(ref.uuid());
        } else {
            return UUID.randomUUID().toString();
        }
    }

    record AccessorDescriptor(
        @NotNull String semantic,
        @NotNull ElementType elementType,
        @NotNull ComponentType componentType,
        boolean unsigned,
        boolean normalized
    ) {}

    record DrawFlags(
        @NotNull String renderType,
        @NotNull String shadowCullMode,
        @NotNull String shadowBiasMode,
        @NotNull String viewLayer,
        float shadowBiasMultiplier,
        boolean castShadow,
        boolean disableOcclusionCulling,
        boolean voxelizeLightBake
    ) {
        @NotNull
        public static DrawFlags valueOf(int flags, @NotNull RTTITypeRegistry registry) {
            final RTTITypeEnum eDrawPartType = registry.find("EDrawPartType");
            final RTTITypeEnum eShadowCull = registry.find("EShadowCull");
            final RTTITypeEnum eViewLayer = registry.find("EViewLayer");
            final RTTITypeEnum eShadowBiasMode = registry.find("EShadowBiasMode");

            final var castShadow = (flags & 1) > 0;
            final var renderType = eDrawPartType.valueOf((flags >>> 3) & 1).name();
            final var shadowCullMode = eShadowCull.valueOf((flags >>> 1) & 3).name();
            final var viewLayer = eViewLayer.valueOf((flags >>> 4) & 3).name();
            final var shadowBiasMultiplier = IOUtils.halfToFloat(((flags >>> 6) & 65535));
            final var shadowBiasMode = eShadowBiasMode.valueOf((flags >>> 22) & 1).name();
            final var disableOcclusionCulling = ((flags >>> 24) & 1) > 0;
            final var voxelizeLightBake = (flags & 0x2000000) > 0;

            return new DrawFlags(renderType, shadowCullMode, shadowBiasMode, viewLayer, shadowBiasMultiplier, castShadow, disableOcclusionCulling, voxelizeLightBake);
        }
    }
}
