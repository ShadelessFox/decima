package com.shade.decima.math;

public record Vec3(float x, float y, float z) {
    private static final Vec3 zero = new Vec3(0, 0, 0);

    public static Vec3 zero() {
        return zero;
    }

    public Vec3 add(Vec3 other) {
        return add(other.x, other.y, other.z);
    }

    public Vec3 add(float x, float y, float z) {
        return new Vec3(this.x + x, this.y + y, this.z + z);
    }

    public Vec3 sub(Vec3 other) {
        return sub(other.x, other.y, other.z);
    }

    public Vec3 sub(float x, float y, float z) {
        return new Vec3(this.x - x, this.y - y, this.z - z);
    }

    public Vec3 mul(float scalar) {
        return mul(scalar, scalar, scalar);
    }

    public Vec3 mul(Vec3 other) {
        return mul(other.x, other.y, other.z);
    }

    public Vec3 mul(float x, float y, float z) {
        return new Vec3(this.x * x, this.y * y, this.z * z);
    }

    public Vec3 normalize() {
        float length = 1.0f / length();
        return new Vec3(x * length, y * length, z * length);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3 cross(Vec3 other) {
        return new Vec3(
            Math.fma(y, other.z, -z * other.y),
            Math.fma(z, other.x, -x * other.z),
            Math.fma(x, other.y, -y * other.x)
        );
    }
}
