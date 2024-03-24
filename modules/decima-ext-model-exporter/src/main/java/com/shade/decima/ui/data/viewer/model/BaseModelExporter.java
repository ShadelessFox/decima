package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;
import org.joml.*;

import java.util.UUID;

public class BaseModelExporter {
    @NotNull
    protected static Matrix4dc worldTransformToMatrix(@NotNull RTTIObject transform) {
        final var pos = transform.obj("Position");
        final var ori = transform.obj("Orientation");
        final var col0 = ori.obj("Col0");
        final var col1 = ori.obj("Col1");
        final var col2 = ori.obj("Col2");
        final Matrix3dc tmpMatrix = new Matrix3d(
            col0.f32("X"), col0.f32("Y"), col0.f32("Z"),
            col1.f32("X"), col1.f32("Y"), col1.f32("Z"),
            col2.f32("X"), col2.f32("Y"), col2.f32("Z")
        ).transpose();
        final Matrix4dc rotationMatrix = new Matrix4d(tmpMatrix);
        Vector3d translation = new Vector3d(
            pos.f64("X"),
            pos.f64("Y"),
            pos.f64("Z")
        );
        final Matrix4d translationMatrix = new Matrix4d().translate(translation);
        translationMatrix.mul(rotationMatrix, translationMatrix);
        return translationMatrix;
    }

    @NotNull
    protected static Matrix4dc mat34ToMatrix(@NotNull RTTIObject object) {
        final RTTIObject row0 = object.obj("Row0");
        final RTTIObject row1 = object.obj("Row1");
        final RTTIObject row2 = object.obj("Row2");

        return new Matrix4d(
            row0.f32("X"), row0.f32("Y"), row0.f32("Z"), row0.f32("W"),
            row1.f32("X"), row1.f32("Y"), row1.f32("Z"), row1.f32("W"),
            row2.f32("X"), row2.f32("Y"), row2.f32("Z"), row2.f32("W"),
            0.0, 0.0, 0.0, 1.0
        );
    }


    @NotNull
    protected static Matrix4dc mat44TransformToMatrix4(@NotNull RTTIObject transform) {
        final var col0 = transform.obj("Col0");
        final var col1 = transform.obj("Col1");
        final var col2 = transform.obj("Col2");
        final var col3 = transform.obj("Col3");

        return new Matrix4d(
            col0.f32("X"), col1.f32("X"), col2.f32("X"), col3.f32("X"),
            col0.f32("Y"), col1.f32("Y"), col2.f32("Y"), col3.f32("Y"),
            col0.f32("Z"), col1.f32("Z"), col2.f32("Z"), col3.f32("Z"),
            col0.f32("W"), col1.f32("W"), col2.f32("W"), col3.f32("W")
        ).transpose();
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
    protected static String uuidToString(@NotNull RTTIReference uuid) {
        if (uuid instanceof RTTIReference.External ref) {
            return RTTIUtils.uuidToString(ref.uuid());
        } else if (uuid instanceof RTTIReference.Internal ref) {
            return RTTIUtils.uuidToString(ref.uuid());
        } else {
            return UUID.randomUUID().toString();
        }
    }

    protected record DrawFlags(
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
            final var shadowBiasMultiplier = MathUtils.halfToFloat(((flags >>> 6) & 65535));
            final var shadowBiasMode = eShadowBiasMode.valueOf((flags >>> 22) & 1).name();
            final var disableOcclusionCulling = ((flags >>> 24) & 1) > 0;
            final var voxelizeLightBake = (flags & 0x2000000) > 0;

            return new DrawFlags(renderType, shadowCullMode, shadowBiasMode, viewLayer, shadowBiasMultiplier, castShadow, disableOcclusionCulling, voxelizeLightBake);
        }
    }
}
