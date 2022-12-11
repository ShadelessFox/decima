package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.data.handlers.GGUUIDValueHandler;
import com.shade.decima.ui.data.viewer.model.utils.Matrix4x4;
import com.shade.decima.ui.data.viewer.model.utils.Transform;
import com.shade.util.NotNull;

public class ModelExporterShared {

    @NotNull
    protected static Transform worldTransformToMatrix(RTTIObject transformObj) {
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
    protected static Matrix4x4 InvertedMatrix4x4TransformToMatrix(RTTIObject transformObj) {
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

    protected static String uuidToString(RTTIObject uuid) {
        return GGUUIDValueHandler.INSTANCE.getString(uuid.type(), uuid);
    }
}
