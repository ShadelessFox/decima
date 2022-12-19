package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.decima.ui.data.viewer.model.utils.*;
import com.shade.util.NotNull;

public class ModelExporterShared {

    @NotNull
    static Transform worldTransformToTransform(RTTIObject transformObj) {
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
    static Matrix4x4 worldTransformToMatrix(RTTIObject transformObj) {
        final var posObj = transformObj.obj("Position");
        final var oriObj = transformObj.obj("Orientation");
        final RTTIObject col0Obj = oriObj.obj("Col0");
        final RTTIObject col1Obj = oriObj.obj("Col1");
        final RTTIObject col2Obj = oriObj.obj("Col2");
        final double[] col0 = {col0Obj.f32("X"), col0Obj.f32("Y"), col0Obj.f32("Z")};
        final double[] col1 = {col1Obj.f32("X"), col1Obj.f32("Y"), col1Obj.f32("Z")};
        final double[] col2 = {col2Obj.f32("X"), col2Obj.f32("Y"), col2Obj.f32("Z")};
        double[] pos = {posObj.f64("X"), posObj.f64("Y"), posObj.f64("Z")};

        Matrix3x3 mat = new Matrix3x3(new double[][]{col0, col1, col2});
        return Matrix4x4.Translation(new Vector3(pos)).matMul(mat.to4x4());
    }

    @NotNull
    static Matrix4x4 InvertedMatrix4x4TransformToMatrix(RTTIObject transformObj) {
        final RTTIObject col0Obj = transformObj.obj("Col0");
        final RTTIObject col1Obj = transformObj.obj("Col1");
        final RTTIObject col2Obj = transformObj.obj("Col2");
        final RTTIObject col3Obj = transformObj.obj("Col3");
        final double[] col0 = {col0Obj.f32("X"), col0Obj.f32("Y"), col0Obj.f32("Z"), col0Obj.f32("W")};
        final double[] col1 = {col1Obj.f32("X"), col1Obj.f32("Y"), col1Obj.f32("Z"), col1Obj.f32("W")};
        final double[] col2 = {col2Obj.f32("X"), col2Obj.f32("Y"), col2Obj.f32("Z"), col2Obj.f32("W")};
        final double[] col3 = {col3Obj.f32("X"), col3Obj.f32("Y"), col3Obj.f32("Z"), col3Obj.f32("W")};
        return new Matrix4x4(new double[][]{col0, col1, col2, col3}).transposed();
    }

    static String nameFromReference(@NotNull RTTIReference ref, @NotNull String resourceName) {
        if (ref.type() == RTTIReference.Type.EXTERNAL_LINK) {
            String path = ref.path();
            if (path == null)
                return resourceName;
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return resourceName;
    }

    static String uuidToString(RTTIObject uuid) {
        return GGUUIDValueHandler.INSTANCE.getString(uuid.type(), uuid);
    }

    record AccessorDescriptor(
        @NotNull String semantic,
        @NotNull ElementType elementType,
        @NotNull ComponentType componentType,
        boolean unsigned,
        boolean normalized
    ) {}

    static class DrawFlags {
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
