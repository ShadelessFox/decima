package com.shade.decima.ui.data.viewer.model.model.utils;

public class Transform {
    private Vector3 translation;
    private Quaternion rotation;
    private Vector3 scale;


    public Transform(double[] translation, double[] rotation, double[] scale) {
        this.translation = new Vector3(translation);
        this.rotation = new Quaternion(rotation);
        this.scale = new Vector3(scale);
    }

    public Transform(Vector3 translation, Quaternion rotation, Vector3 scale) {
        this.translation = translation;
        this.rotation = rotation;
        this.scale = scale;
    }

    public Transform() {
        this.translation = new Vector3(0, 0, 0);
        this.rotation = Quaternion.Identity();
        this.scale = new Vector3(1, 1, 1);
    }

    public double[] getTranslation() {
        return translation.toArray();
    }

    public void setTranslation(double[] translation) {
        this.translation = new Vector3(translation);
    }

    public double[] getRotation() {
        return rotation.toArray();
    }

    public void setRotation(double[] rotation) {
        this.rotation = new Quaternion(rotation);
    }

    public double[] getScale() {
        return scale.toArray();
    }

    public void setScale(double[] scale) {
        this.scale = new Vector3(scale);
    }

    public boolean isIdentity() {
        return translation.x() == 0 && translation.y() == 0 && translation.z() == 0 &&
               rotation.x() == 0 && rotation.y() == 0 && rotation.z() == 0 && rotation.w() == 1 &&
               scale.x() == 1 && scale.y() == 1 && scale.z() == 1;
    }


    public static Transform fromRotationAndScaleMatrix(double[][] matrix) {
        Matrix3x3 mat = new Matrix3x3(matrix);
        return new Transform(new Vector3(), mat.toQuaternion(), mat.toScale());
    }

    public static Transform fromTranslationAndRotationAndScaleMatrix(double[][] matrix) {
        Matrix3x3 mat = new Matrix3x3(matrix);
        Vector3 translation = new Vector3(matrix[3]);
        return new Transform(translation, mat.toQuaternion(), mat.toScale());
    }

    public static Transform fromRotation(double pitchDeg, double yawDeg, double rollDeg) {
        Transform transform = new Transform();
        transform.rotation = Quaternion.fromEuler(yawDeg, pitchDeg, rollDeg);
        return transform;
    }

    public static Transform fromMatrixAndTranslation(double[][] matrix, double[] translation) {
        Transform transform = fromRotationAndScaleMatrix(matrix);
        transform.translation = new Vector3(translation);
        return transform;
    }
}
