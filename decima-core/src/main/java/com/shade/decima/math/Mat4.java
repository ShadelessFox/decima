package com.shade.decima.math;

import java.nio.FloatBuffer;

public record Mat4(
    float m00, float m01, float m02, float m03,
    float m10, float m11, float m12, float m13,
    float m20, float m21, float m22, float m23,
    float m30, float m31, float m32, float m33
) {
    public static final int BYTES = Float.BYTES * 16;

    private static final Mat4 identity = new Mat4(
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    );

    public static Mat4 identity() {
        return identity;
    }

    public static Mat4 rotationX(float ang) {
        float sin = (float) Math.sin(ang);
        float cos = (float) Math.cos(ang);
        return new Mat4(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, +cos, -sin, 0.0f,
            0.0f, +sin, -cos, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        );
    }

    public static Mat4 perspective(float fov, float aspect, float zNear, float zFar) {
        float h = (float) Math.tan(fov * 0.5f);
        float m00 = 1.0f / (h * aspect);
        float m11 = 1.0f / h;
        float m22 = (zFar + zNear) / (zNear - zFar);
        float m32 = (zFar + zFar) * zNear / (zNear - zFar);
        float m23 = -1.0f;

        return new Mat4(
            m00, 0.f, 0.f, 0.f,
            0.f, m11, 0.f, 0.f,
            0.f, 0.f, m22, m23,
            0.f, 0.f, m32, 0.f
        );
    }

    public static Mat4 lookAt(Vec3 eye, Vec3 center, Vec3 up) {
        Vec3 dir = eye.sub(center).normalize();
        Vec3 left = up.cross(dir).normalize();
        Vec3 upn = dir.cross(left);

        Mat4 result = new Mat4(
            left.x(), upn.x(), dir.x(), 0.f,
            left.y(), upn.y(), dir.y(), 0.f,
            left.z(), upn.z(), dir.z(), 0.f,
            0.f, 0.f, 0.f, 1.f
        );

        return result.translate(-eye.x(), -eye.y(), -eye.z());
    }

    public Mat4 rotateX(float ang) {
        return mul(rotationX(ang));
    }

    public Mat4 translate(float x, float y, float z) {
        float m30 = Math.fma(m00(), x, Math.fma(m10(), y, Math.fma(m20(), z, m30())));
        float m31 = Math.fma(m01(), x, Math.fma(m11(), y, Math.fma(m21(), z, m31())));
        float m32 = Math.fma(m02(), x, Math.fma(m12(), y, Math.fma(m22(), z, m32())));
        float m33 = Math.fma(m03(), x, Math.fma(m13(), y, Math.fma(m23(), z, m33())));

        return new Mat4(
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        );
    }

    public Mat4 scale(float xyz) {
        return scale(xyz, xyz, xyz);
    }

    public Mat4 scale(float x, float y, float z) {
        return new Mat4(
            m00 * x, m01 * x, m02 * x, m03 * x,
            m10 * y, m11 * y, m12 * y, m13 * y,
            m20 * z, m21 * z, m22 * z, m23 * z,
            m30, m31, m32, m33
        );
    }

    public Mat4 mul(Mat4 o) {
        var m00 = Math.fma(m00(), o.m00(), Math.fma(m10(), o.m01(), Math.fma(m20(), o.m02(), m30() * o.m03())));
        var m01 = Math.fma(m01(), o.m00(), Math.fma(m11(), o.m01(), Math.fma(m21(), o.m02(), m31() * o.m03())));
        var m02 = Math.fma(m02(), o.m00(), Math.fma(m12(), o.m01(), Math.fma(m22(), o.m02(), m32() * o.m03())));
        var m03 = Math.fma(m03(), o.m00(), Math.fma(m13(), o.m01(), Math.fma(m23(), o.m02(), m33() * o.m03())));
        var m10 = Math.fma(m00(), o.m10(), Math.fma(m10(), o.m11(), Math.fma(m20(), o.m12(), m30() * o.m13())));
        var m11 = Math.fma(m01(), o.m10(), Math.fma(m11(), o.m11(), Math.fma(m21(), o.m12(), m31() * o.m13())));
        var m12 = Math.fma(m02(), o.m10(), Math.fma(m12(), o.m11(), Math.fma(m22(), o.m12(), m32() * o.m13())));
        var m13 = Math.fma(m03(), o.m10(), Math.fma(m13(), o.m11(), Math.fma(m23(), o.m12(), m33() * o.m13())));
        var m20 = Math.fma(m00(), o.m20(), Math.fma(m10(), o.m21(), Math.fma(m20(), o.m22(), m30() * o.m23())));
        var m21 = Math.fma(m01(), o.m20(), Math.fma(m11(), o.m21(), Math.fma(m21(), o.m22(), m31() * o.m23())));
        var m22 = Math.fma(m02(), o.m20(), Math.fma(m12(), o.m21(), Math.fma(m22(), o.m22(), m32() * o.m23())));
        var m23 = Math.fma(m03(), o.m20(), Math.fma(m13(), o.m21(), Math.fma(m23(), o.m22(), m33() * o.m23())));
        var m30 = Math.fma(m00(), o.m30(), Math.fma(m10(), o.m31(), Math.fma(m20(), o.m32(), m30() * o.m33())));
        var m31 = Math.fma(m01(), o.m30(), Math.fma(m11(), o.m31(), Math.fma(m21(), o.m32(), m31() * o.m33())));
        var m32 = Math.fma(m02(), o.m30(), Math.fma(m12(), o.m31(), Math.fma(m22(), o.m32(), m32() * o.m33())));
        var m33 = Math.fma(m03(), o.m30(), Math.fma(m13(), o.m31(), Math.fma(m23(), o.m32(), m33() * o.m33())));

        return new Mat4(
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        );
    }

    public FloatBuffer get(FloatBuffer dst) {
        dst.put(m00).put(m01).put(m02).put(m03);
        dst.put(m10).put(m11).put(m12).put(m13);
        dst.put(m20).put(m21).put(m22).put(m23);
        dst.put(m30).put(m31).put(m32).put(m33);
        return dst;
    }

    @Override
    public String toString() {
        return "(" + m00 + ", " + m10 + ", " + m20 + ", " + m30 + ")\n" +
            "(" + m01 + ", " + m11 + ", " + m21 + ", " + m31 + ")\n" +
            "(" + m02 + ", " + m12 + ", " + m22 + ", " + m32 + ")\n" +
            "(" + m03 + ", " + m13 + ", " + m23 + ", " + m33 + ")";
    }
}
