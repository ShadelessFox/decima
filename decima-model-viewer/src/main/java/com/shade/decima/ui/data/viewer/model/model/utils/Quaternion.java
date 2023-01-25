package com.shade.decima.ui.data.viewer.model.model.utils;

public class Quaternion {
    private final double[] storage;

    public Quaternion(double x, double y, double z, double w) {
        storage = new double[]{x, y, z, w};
    }

    public Quaternion(double[] xyzw) {
        storage = new double[4];
        System.arraycopy(xyzw, 0, storage, 0, 4);
    }

    public static Quaternion Identity() {
        return new Quaternion(0, 0, 0, 1);
    }

    public static Quaternion fromEuler(double yaw, double pitch, double roll) {
        double[] e = new double[]{Math.toRadians(yaw), Math.toRadians(pitch), Math.toRadians(roll)};
        double ti = e[0] * 0.5f;
        double tj = e[1] * -0.5f;
        double th = e[2] * 0.5f;

        double ci = Math.cos(ti);
        double cj = Math.cos(tj);
        double ch = Math.cos(th);
        double si = Math.sin(ti);
        double sj = Math.sin(tj);
        double sh = Math.sin(th);

        double cc = ci * ch;
        double cs = ci * sh;
        double sc = si * ch;
        double ss = si * sh;
        double[] a = new double[3];

        a[0] = cj * sc - sj * cs;
        a[1] = cj * ss + sj * cc;
        a[2] = cj * cs - sj * sc;

        double w = cj * cc + sj * ss;
        return new Quaternion(a[0], a[1], a[2], w);
    }

    public String toString() {
        return "Quaternion(%.4f, %.4f, %.4f, %.4f)".formatted(storage[0],storage[1],storage[2],storage[3]);
    }

    public double[] toArray() {
        return storage;
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

    public double w() {
        return storage[3];
    }


    public double magnitude() {
        return Math.sqrt(x() * x() + y() * y() + z() * z() + w() * w());
    }

    public Quaternion normalized() {
        double norm = magnitude();
        return new Quaternion(x() / norm, y() / norm, z() / norm, w() / norm);
    }

    public Quaternion scaled(double scale) {
        double[] q = toArray();
        for (int i = 0; i < q.length; i++) {
            q[i] *= scale;
        }
        return new Quaternion(q);
    }

    public Quaternion conjugate() {
        return new Quaternion(-x(), -y(), -z(), w());
    }

    public Quaternion add(Quaternion other) {
        return new Quaternion(x() + other.x(), y() + other.y(), z() + other.y(), w() + other.w());
    }

    public Quaternion mul(Quaternion other) {

        return new Quaternion(x() * other.x(), y() * other.y(), z() * other.z(), w() * other.w());
    }

    public Quaternion matMul(Quaternion other) {
        double[] tA = toArray();
        double[] tB = other.toArray();
        double[] a = new double[]{tA[3], tA[0], tA[1], tA[2]};
        double[] b = new double[]{tB[3], tB[0], tB[1], tB[2]};
        double t0 = a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3];
        double t1 = a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2];
        double t2 = a[0] * b[2] + a[2] * b[0] + a[3] * b[1] - a[1] * b[3];

        return new Quaternion(t1, t2, a[0] * b[3] + a[3] * b[0] + a[1] * b[2] - a[2] * b[1], t0);
    }

    public Quaternion inverse() {
        double d = magnitude();
        return new Quaternion(x() / d, -y() / d, -z() / d, -w() / d);
    }

    public Quaternion div(Quaternion b) {
        return mul(b.inverse());
    }

    public Quaternion rotate(Quaternion other) {
        Matrix3x3 otherRMat = other.toMatrix();
        Matrix3x3 selfMat = normalized().toMatrix();
        Matrix3x3 rMat = otherRMat.matMul(selfMat);
        return rMat.toQuaternion().scaled(magnitude());
    }

    public Vector3 toEuler() {
        return toMatrix().toEuler();
    }

    public Matrix3x3 toMatrix() {
        final double sqrt2 = 1.41421356237309504880;
        double[][] matrix = new double[3][3];
        double q0, q1, q2, q3, qda, qdb, qdc, qaa, qab, qac, qbb, qbc, qcc;

        q0 = sqrt2 * w();
        q1 = sqrt2 * x();
        q2 = sqrt2 * y();
        q3 = sqrt2 * z();

        qda = q0 * q1;
        qdb = q0 * q2;
        qdc = q0 * q3;
        qaa = q1 * q1;
        qab = q1 * q2;
        qac = q1 * q3;
        qbb = q2 * q2;
        qbc = q2 * q3;
        qcc = q3 * q3;

        matrix[0][0] = (1.0 - qbb - qcc);
        matrix[0][1] = (qdc + qab);
        matrix[0][2] = (-qdb + qac);

        matrix[1][0] = (-qdc + qab);
        matrix[1][1] = (1.0 - qaa - qcc);
        matrix[1][2] = (qda + qbc);

        matrix[2][0] = (qdb + qac);
        matrix[2][1] = (-qda + qbc);
        matrix[2][2] = (1.0 - qaa - qbb);
        return new Matrix3x3(matrix);
    }

    public Vector3 toVec3() {
        return new Vector3(x(), y(), z());
    }
}
