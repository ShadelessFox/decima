package com.shade.decima.ui.data.viewer.mesh.utils;

public class Vector3 {
    private final double[] storage;


    public Vector3(double x, double y, double z) {
        storage = new double[]{x, y, z};
    }

    public Vector3(double[] xyz) {
        storage = new double[3];
        System.arraycopy(xyz, 0, storage, 0, 3);
    }

    public Vector3() {
        this(0, 0, 0);
    }

    public String toString() {
        return "Vector3(%.3f, %.3f, %.3f)".formatted(storage[0], storage[1], storage[2]);
    }

    public double x() {
        return storage[0];
    }

    public double y() {
        return storage[1];
    }

    public double z() {
        return storage[2];
    }

    public Vector3 add(Vector3 other) {
        return new Vector3(storage[0] + other.storage[0], storage[1] + other.storage[1], storage[2] + other.storage[2]);
    }

    public Vector3 sub(Vector3 other) {
        return new Vector3(storage[0] - other.storage[0], storage[1] - other.storage[1], storage[2] - other.storage[2]);
    }

    public Vector3 mul(Vector3 other) {
        return new Vector3(storage[0] * other.storage[0], storage[1] * other.storage[1], storage[2] * other.storage[2]);
    }

    public Vector3 div(Vector3 other) {
        return new Vector3(storage[0] / other.storage[0], storage[1] / other.storage[1], storage[2] / other.storage[2]);
    }

    public Vector3 cross(Vector3 other) {
        double newX = storage[1] * other.z() - storage[2] * other.y();
        double newY = storage[2] * other.x() - storage[0] * other.z();
        double newZ = storage[0] * other.y() - storage[1] * other.x();
        return new Vector3(newX, newY, newZ);
    }

    public double dot(Vector3 other) {
        return storage[0] * other.storage[0] + storage[1] * other.storage[1] + storage[2] * other.storage[2];
    }

    public double magnitude() {
        return Math.sqrt(storage[0] * storage[0] + storage[1] * storage[1] + storage[2] * storage[2]);
    }

    public Vector3 normalized() {
        double norm = magnitude();
        return new Vector3(storage[0] / norm, storage[1] / norm, storage[2] / norm);
    }

    public Vector3 rotate(Quaternion quat) {
        Quaternion q_v = quat.matMul(toVec4());
        Quaternion conjugate = quat.conjugate();
        return q_v.matMul(conjugate).toVec3();
    }

    public Matrix4x4 toTranslationMatrix() {
        return Matrix4x4.Translation(this);
    }

    public Matrix4x4 toScaleMatrix() {
        return Matrix4x4.Scale(this);
    }

    public Quaternion toVec4() {
        return new Quaternion(x(), y(), z(), 0);
    }

    public double[] toArray() {
        return new double[]{x(), y(), z()};
    }
}
