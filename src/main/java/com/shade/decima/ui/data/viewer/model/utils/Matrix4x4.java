package com.shade.decima.ui.data.viewer.model.utils;


public class Matrix4x4 {
    double[][] matrix;

    public Matrix4x4() {
        this.matrix = new double[4][4];
    }

    public Matrix4x4(double[][] matrix) {
        this.matrix = new double[4][4];
        System.arraycopy(matrix[0], 0, this.matrix[0], 0, 4);
        System.arraycopy(matrix[1], 0, this.matrix[1], 0, 4);
        System.arraycopy(matrix[2], 0, this.matrix[2], 0, 4);
        System.arraycopy(matrix[3], 0, this.matrix[3], 0, 4);
    }

    public static Matrix4x4 Unit() {
        Matrix4x4 m = new Matrix4x4();
        for (int i = 0; i < 4; i++) {
            m.set(i, i, 1);
        }
        return m;
    }

    private void set(int x, int y, double value) {
        matrix[x][y] = value;
    }

    public static Matrix4x4 Translation(Vector3 translation) {
        Matrix4x4 matrix = Matrix4x4.Unit();
        matrix.matrix[0][3] = translation.x();
        matrix.matrix[1][3] = translation.y();
        matrix.matrix[2][3] = translation.z();
        return matrix;
    }

    public static Matrix4x4 Rotation(double angle, Vector3 axis) {
        if (axis.magnitude() == 0) {
            return Matrix4x4.Unit();
        }
        axis = axis.normalized();
        double angle_cos = Math.cos(angle);
        double angle_sin = Math.sin(angle);
        double ico = (1.0f - angle_cos);
        double[] nsi = new double[3];
        nsi[0] = axis.x() * angle_sin;
        nsi[1] = axis.y() * angle_sin;
        nsi[2] = axis.z() * angle_sin;

        double n_00 = (axis.x() * axis.x()) * ico;
        double n_01 = (axis.x() * axis.y()) * ico;
        double n_11 = (axis.y() * axis.y()) * ico;
        double n_02 = (axis.x() * axis.z()) * ico;
        double n_12 = (axis.y() * axis.z()) * ico;
        double n_22 = (axis.z() * axis.z()) * ico;
        Matrix4x4 matrix = Matrix4x4.Unit();
        matrix.matrix[0][0] = n_00 + angle_cos;
        matrix.matrix[0][1] = n_01 + nsi[2];
        matrix.matrix[0][2] = n_02 - nsi[1];
        matrix.matrix[1][0] = n_01 - nsi[2];
        matrix.matrix[1][1] = n_11 + angle_cos;
        matrix.matrix[1][2] = n_12 + nsi[0];
        matrix.matrix[2][0] = n_02 + nsi[1];
        matrix.matrix[2][1] = n_12 - nsi[0];
        matrix.matrix[2][2] = n_22 + angle_cos;
        return matrix;
    }

    public static Matrix4x4 Scale(Vector3 vector3) {
        throw new RuntimeException();
    }

    public double get(int col, int row) {
        return matrix[row][col];
    }

    public Matrix4x4 mul(Matrix4x4 other) {
        double[][] m1 = this.matrix;
        double[][] m2 = other.matrix;
        Matrix4x4 m3 = new Matrix4x4();
        m3.set(0, 0, m2[0][0] * m1[0][0] + m2[0][1] * m1[1][0] + m2[0][2] * m1[2][0]);
        m3.set(0, 1, m2[0][0] * m1[0][1] + m2[0][1] * m1[1][1] + m2[0][2] * m1[2][1]);
        m3.set(0, 2, m2[0][0] * m1[0][2] + m2[0][1] * m1[1][2] + m2[0][2] * m1[2][2]);
        m3.set(0, 3, m2[0][0] * m1[0][3] + m2[0][1] * m1[1][3] + m2[0][3] * m1[2][3]);
        m3.set(1, 0, m2[1][0] * m1[0][0] + m2[1][1] * m1[1][0] + m2[1][2] * m1[2][0]);
        m3.set(1, 1, m2[1][0] * m1[0][1] + m2[1][1] * m1[1][1] + m2[1][2] * m1[2][1]);
        m3.set(1, 2, m2[1][0] * m1[0][2] + m2[1][1] * m1[1][2] + m2[1][2] * m1[2][2]);
        m3.set(1, 3, m2[1][0] * m1[0][3] + m2[1][1] * m1[1][3] + m2[1][2] * m1[2][3]);
        m3.set(2, 0, m2[2][0] * m1[0][0] + m2[2][1] * m1[1][0] + m2[2][2] * m1[2][0]);
        m3.set(2, 1, m2[2][0] * m1[0][1] + m2[2][1] * m1[1][1] + m2[2][2] * m1[2][1]);
        m3.set(2, 2, m2[2][0] * m1[0][2] + m2[2][1] * m1[1][2] + m2[2][2] * m1[2][2]);
        m3.set(2, 3, m2[2][0] * m1[0][3] + m2[2][1] * m1[1][3] + m2[2][2] * m1[2][3]);
        m3.set(3, 0, m2[3][0] * m1[0][0] + m2[3][1] * m1[1][0] + m2[3][2] * m1[2][0]);
        m3.set(3, 1, m2[3][0] * m1[0][1] + m2[3][1] * m1[1][1] + m2[3][2] * m1[2][1]);
        m3.set(3, 2, m2[3][0] * m1[0][2] + m2[3][1] * m1[1][2] + m2[3][2] * m1[2][2]);
        m3.set(3, 3, m2[3][0] * m1[0][3] + m2[3][1] * m1[1][3] + m2[3][2] * m1[2][3]);
        return m3;
    }

    public Matrix4x4 matMul(Matrix4x4 other) {
        Matrix4x4 otherT = other.transposed();
        Matrix4x4 mat = new Matrix4x4();
        for (int col = 0; col < matrix.length; col++) {
            for (int row = 0; row < matrix.length; row++) {
                double dot = MathUtils.dotProduct(matrix[row], otherT.matrix[col]);
                mat.set(col, row, dot);
            }
        }
        return mat.transposed();
    }

    public Matrix4x4 negative() {
        Matrix4x4 output = Matrix4x4.Unit();
        for (int i = 0; i < matrix.length; i++) {
            double[] col = matrix[i];
            for (int j = 0; j < col.length; j++) {
                output.matrix[i][j] = -col[j];
            }
        }
        return output;
    }

    public Matrix4x4 normalized() {
        Matrix4x4 output = Matrix4x4.Unit();
        for (int i = 0; i < matrix.length; i++) {
            double[] col = matrix[i];
            double mag = 0;
            for (double v : col) {
                mag += v * v;
            }
            mag = Math.sqrt(mag);
            for (int j = 0; j < col.length; j++) {
                output.matrix[i][j] = col[j] / mag;
            }
        }
        return output;
    }

    public Matrix4x4 transposed() {
        double[][] transposed = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return new Matrix4x4(transposed);
    }

    public Matrix4x4 adjoint() {
        double[][] srcMat = matrix;
        Matrix4x4 res = Matrix4x4.Unit();
        double[][] dstMat = res.matrix;

        double a1 = srcMat[0][0];
        double b1 = srcMat[0][1];
        double c1 = srcMat[0][2];
        double d1 = srcMat[0][3];

        double a2 = srcMat[1][0];
        double b2 = srcMat[1][1];
        double c2 = srcMat[1][2];
        double d2 = srcMat[1][3];

        double a3 = srcMat[2][0];
        double b3 = srcMat[2][1];
        double c3 = srcMat[2][2];
        double d3 = srcMat[2][3];

        double a4 = srcMat[3][0];
        double b4 = srcMat[3][1];
        double c4 = srcMat[3][2];
        double d4 = srcMat[3][3];

        dstMat[0][0] = Matrix3x3.determinant3x3(new double[][]{{b2, b3, b4}, {c2, c3, c4}, {d2, d3, d4}});
        dstMat[1][0] = -Matrix3x3.determinant3x3(new double[][]{{a2, a3, a4}, {c2, c3, c4}, {d2, d3, d4}});
        dstMat[2][0] = Matrix3x3.determinant3x3(new double[][]{{a2, a3, a4}, {b2, b3, b4}, {d2, d3, d4}});
        dstMat[3][0] = -Matrix3x3.determinant3x3(new double[][]{{a2, a3, a4}, {b2, b3, b4}, {c2, c3, c4}});

        dstMat[0][1] = -Matrix3x3.determinant3x3(new double[][]{{b1, b3, b4}, {c1, c3, c4}, {d1, d3, d4}});
        dstMat[1][1] = Matrix3x3.determinant3x3(new double[][]{{a1, a3, a4}, {c1, c3, c4}, {d1, d3, d4}});
        dstMat[2][1] = -Matrix3x3.determinant3x3(new double[][]{{a1, a3, a4}, {b1, b3, b4}, {d1, d3, d4}});
        dstMat[3][1] = Matrix3x3.determinant3x3(new double[][]{{a1, a3, a4}, {b1, b3, b4}, {c1, c3, c4}});

        dstMat[0][2] = Matrix3x3.determinant3x3(new double[][]{{b1, b2, b4}, {c1, c2, c4}, {d1, d2, d4}});
        dstMat[1][2] = -Matrix3x3.determinant3x3(new double[][]{{a1, a2, a4}, {c1, c2, c4}, {d1, d2, d4}});
        dstMat[2][2] = Matrix3x3.determinant3x3(new double[][]{{a1, a2, a4}, {b1, b2, b4}, {d1, d2, d4}});
        dstMat[3][2] = -Matrix3x3.determinant3x3(new double[][]{{a1, a2, a4}, {b1, b2, b4}, {c1, c2, c4}});

        dstMat[0][3] = -Matrix3x3.determinant3x3(new double[][]{{b1, b2, b3}, {c1, c2, c3}, {d1, d2, d3}});
        dstMat[1][3] = Matrix3x3.determinant3x3(new double[][]{{a1, a2, a3}, {c1, c2, c3}, {d1, d2, d3}});
        dstMat[2][3] = -Matrix3x3.determinant3x3(new double[][]{{a1, a2, a3}, {b1, b2, b3}, {d1, d2, d3}});
        dstMat[3][3] = Matrix3x3.determinant3x3(new double[][]{{a1, a2, a3}, {b1, b2, b3}, {c1, c2, c3}});
        return res;
    }

    public double determinant() {
        double determinant1 = Matrix3x3.determinant3x3(new double[][]{
                {matrix[1][1], matrix[1][2], matrix[1][3]},
                {matrix[2][1], matrix[2][2], matrix[2][3]},
                {matrix[3][1], matrix[3][2], matrix[3][3]},
            }
        );
        double determinant2 = Matrix3x3.determinant3x3(new double[][]{
                {matrix[1][0], matrix[1][2], matrix[1][3]},
                {matrix[2][0], matrix[2][2], matrix[2][3]},
                {matrix[3][0], matrix[3][2], matrix[3][3]},
            }
        );
        double determinant3 = Matrix3x3.determinant3x3(new double[][]{
                {matrix[1][0], matrix[1][1], matrix[1][3]},
                {matrix[2][0], matrix[2][1], matrix[2][3]},
                {matrix[3][0], matrix[3][1], matrix[3][3]},
            }
        );
        double determinant4 = Matrix3x3.determinant3x3(new double[][]{
                {matrix[1][0], matrix[1][1], matrix[1][2]},
                {matrix[2][0], matrix[2][1], matrix[2][2]},
                {matrix[3][0], matrix[3][1], matrix[3][2]},
            }
        );

        return (
            matrix[0][0] * determinant1
            - matrix[0][1] * determinant2
            + matrix[0][2] * determinant3
            - matrix[0][3] * determinant4
        );
    }

    public Quaternion toQuaternion() {
        return new Matrix3x3(matrix).toQuaternion();
    }

    private void toEuler2(double[] eul1, double[] eul2) {
        double cy = Math.hypot(matrix[0][0], matrix[0][1]);


        if (cy > 16.0f * Double.longBitsToDouble(971L << 52)) {

            eul1[0] = Math.atan2(matrix[1][2], matrix[2][2]);
            eul1[1] = Math.atan2(-matrix[0][2], cy);
            eul1[2] = Math.atan2(matrix[0][1], matrix[0][0]);

            eul2[0] = Math.atan2(-matrix[1][2], -matrix[2][2]);
            eul2[1] = Math.atan2(-matrix[0][2], -cy);
            eul2[2] = Math.atan2(-matrix[0][1], -matrix[0][0]);
        } else {
            eul2[0] = eul1[0] = Math.atan2(-matrix[2][1], matrix[1][1]);
            eul2[1] = eul1[1] = Math.atan2(-matrix[0][2], cy);
            eul2[2] = eul1[2] = 0.0f;
        }
    }

    public Vector3 toEuler() {
        double[] eul1 = new double[3], eul2 = new double[3];
        toEuler2(eul1, eul2);

        /* return best, which is just the one with the lowest values it in */
        if (Math.abs(eul1[0]) + Math.abs(eul1[1]) + Math.abs(eul1[2]) >
            Math.abs(eul2[0]) + Math.abs(eul2[1]) + Math.abs(eul2[2])) {
            return new Vector3(eul2);
        } else {
            return new Vector3(eul1);
        }
    }

    public Vector3 toScale() {
        double[] size = new double[3];
        for (int i = 0; i < 3; i++) {
            double[] row = matrix[i];
            double mag = Math.sqrt(row[0] * row[0] + row[1] * row[1] + row[2] * row[2]);
            size[i] = mag;
        }
        if (isNegative()) {
            for (int i = 0; i < size.length; i++) {
                size[i] = -size[i];
            }
        }
        return new Vector3(size);
    }

    public Vector3 toTranslation() {
        return new Vector3(matrix[0][3], matrix[1][3], matrix[2][3]);
    }
    public Vector3 toTranslationGLTF() {
        return new Vector3(matrix[3][0], matrix[3][1], matrix[3][2]);
    }

    public boolean isNegative() {
        return determinant() < 0;
    }

    @Override
    public String toString() {
        return "Matrix4x4(\n\t[%f, %f, %f, %f],\n\t[%f, %f, %f, %f],\n\t[%f, %f, %f, %f],\n\t[%f, %f, %f, %f]\n)".formatted(
            matrix[0][0], matrix[0][1], matrix[0][2], matrix[0][3],
            matrix[1][0], matrix[1][1], matrix[1][2], matrix[1][3],
            matrix[2][0], matrix[2][1], matrix[2][2], matrix[2][3],
            matrix[3][0], matrix[3][1], matrix[3][2], matrix[3][3]
        );
    }

    public Matrix4x4 inverted() {
        Matrix4x4 invertedMatrix = new Matrix4x4(matrix);
        double[][] matSrc = invertedMatrix.matrix;
        final double pseudoinverseEpsilon = 1e-8f;
        double det = determinant();
        if (det == 0) {

            matSrc[0][0] += pseudoinverseEpsilon;
            matSrc[1][1] += pseudoinverseEpsilon;
            matSrc[2][2] += pseudoinverseEpsilon;
            matSrc[3][3] += pseudoinverseEpsilon;
            det = invertedMatrix.determinant();
            if (det == 0) {
                invertedMatrix = Matrix4x4.Unit();
                det = 1;
            }
        }

        Matrix4x4 adjointMatrix = invertedMatrix.adjoint();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                matSrc[j][i] = adjointMatrix.matrix[j][i] / det;
            }
        }
        return invertedMatrix;
    }

    public double[] toArray() {
        double[] output = new double[16];
        System.arraycopy(matrix[0], 0, output, 0, 4);
        System.arraycopy(matrix[1], 0, output, 4, 4);
        System.arraycopy(matrix[2], 0, output, 8, 4);
        System.arraycopy(matrix[3], 0, output, 12, 4);
        return output;
    }

    public boolean isIdentity() {
        return matrix[0][0] == 1. && matrix[1][1] == 1. && matrix[2][2] == 1. && matrix[3][3] == 1.;
    }
}
