package com.shade.decima.ui.data.viewer.model.model.utils;

public class Matrix3x3 {
    double[][] matrix;

    public Matrix3x3(double[][] matrix) {
        this.matrix = new double[3][3];
        System.arraycopy(matrix[0], 0, this.matrix[0], 0, 3);
        System.arraycopy(matrix[1], 0, this.matrix[1], 0, 3);
        System.arraycopy(matrix[2], 0, this.matrix[2], 0, 3);
    }

    public Matrix3x3() {
        this(Unit().matrix);
    }

    public static Matrix3x3 Unit() {
        double[][] m = new double[3][3];
        for (int i = 0; i < 3; i++) {
            m[i][i] = 1;
        }
        return new Matrix3x3(m);
    }

    public static Matrix3x3 Rotation(double angle, Vector3 axis) {
        if (axis.magnitude() == 0) {
            return Matrix3x3.Unit();
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
        Matrix3x3 matrix = Matrix3x3.Unit();
        matrix.matrix[0][0] = n_00 + angle_cos;
        matrix.matrix[1][0] = n_01 + nsi[2];
        matrix.matrix[2][0] = n_02 - nsi[1];

        matrix.matrix[0][1] = n_01 - nsi[2];
        matrix.matrix[1][1] = n_11 + angle_cos;
        matrix.matrix[2][1] = n_12 + nsi[0];

        matrix.matrix[0][2] = n_02 + nsi[1];
        matrix.matrix[1][2] = n_12 - nsi[0];
        matrix.matrix[2][2] = n_22 + angle_cos;
        return matrix;
    }

    public static Matrix3x3 Scale(double factor, Vector3 axis) {
        double[][] mat = new double[3][3];
        Vector3 tvec = axis.normalized();
        mat[0][0] = 1 + ((factor - 1) * (tvec.x() * tvec.x()));
        mat[0][1] = ((factor - 1) * (tvec.x() * tvec.y()));
        mat[0][2] = ((factor - 1) * (tvec.x() * tvec.z()));
        mat[1][0] = ((factor - 1) * (tvec.x() * tvec.y()));
        mat[1][1] = 1 + ((factor - 1) * (tvec.y() * tvec.y()));
        mat[1][2] = ((factor - 1) * (tvec.y() * tvec.z()));
        mat[2][0] = ((factor - 1) * (tvec.x() * tvec.z()));
        mat[2][1] = ((factor - 1) * (tvec.y() * tvec.z()));
        mat[2][2] = 1 + ((factor - 1) * (tvec.z() * tvec.z()));

        return new Matrix3x3(mat);
    }

    public Matrix3x3 mul(Matrix3x3 other) {
        double[][] m1 = this.matrix;
        double[][] m2 = other.matrix;
        double[][] m3 = new double[3][3];
        m3[0][0] = m2[0][0] * m1[0][0] + m2[0][1] * m1[1][0] + m2[0][2] * m1[2][0];
        m3[0][1] = m2[0][0] * m1[0][1] + m2[0][1] * m1[1][1] + m2[0][2] * m1[2][1];
        m3[0][2] = m2[0][0] * m1[0][2] + m2[0][1] * m1[1][2] + m2[0][2] * m1[2][2];

        m3[1][0] = m2[1][0] * m1[0][0] + m2[1][1] * m1[1][0] + m2[1][2] * m1[2][0];
        m3[1][1] = m2[1][0] * m1[0][1] + m2[1][1] * m1[1][1] + m2[1][2] * m1[2][1];
        m3[1][2] = m2[1][0] * m1[0][2] + m2[1][1] * m1[1][2] + m2[1][2] * m1[2][2];

        m3[2][0] = m2[2][0] * m1[0][0] + m2[2][1] * m1[1][0] + m2[2][2] * m1[2][0];
        m3[2][1] = m2[2][0] * m1[0][1] + m2[2][1] * m1[1][1] + m2[2][2] * m1[2][1];
        m3[2][2] = m2[2][0] * m1[0][2] + m2[2][1] * m1[1][2] + m2[2][2] * m1[2][2];
        return new Matrix3x3(m3);
    }

    public Matrix3x3 matMul(Matrix3x3 other) {
        double[][] mat = new double[3][3];
        for (int col = 0; col < matrix.length; col++) {
            for (int row = 0; row < matrix.length; row++) {
                double dot = 0;
                for (int item = 0; item < matrix.length; item++) {
                    dot += matrix[item][row] * other.matrix[col][item];
                }
                mat[col][row] = dot;
            }
        }
        return new Matrix3x3(mat);
    }

    public Matrix3x3 negative() {
        Matrix3x3 output = Matrix3x3.Unit();
        for (int i = 0; i < matrix.length; i++) {
            double[] col = matrix[i];
            for (int j = 0; j < col.length; j++) {
                output.matrix[i][j] = -col[j];
            }
        }
        return output;
    }

    public Matrix3x3 normalized() {
        Matrix3x3 output = Matrix3x3.Unit();
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

    public double determinant() {
        return determinant3x3(matrix);
    }

    public static double determinant3x3(double[][] matrix) {
        return (matrix[0][0] * (matrix[1][1] * matrix[2][2] - matrix[1][2] * matrix[2][1]) -
                matrix[1][0] * (matrix[0][1] * matrix[2][2] - matrix[0][2] * matrix[2][1]) +
                matrix[2][0] * (matrix[0][1] * matrix[1][2] - matrix[0][2] * matrix[1][1]));
    }

    public boolean isNegative() {
        return determinant() < 0;
    }

    public Quaternion toQuaternion() {
        double[][] mat = matrix;
        double det = determinant();
        if (Double.isInfinite(det)) {
            mat = Matrix3x3.Unit().matrix;
        } else if (isNegative()) {
            mat = negative().matrix;
        }

        final double trace = mat[0][0] + mat[1][1] + mat[2][2];
        double[] q = new double[4];
        if (trace > 0) {
            double s = 2.0 * Math.sqrt(1.0 + trace);

            q[0] = 0.25 * s;

            s = 1.0 / s;

            q[1] = (mat[1][2] - mat[2][1]) * s;
            q[2] = (mat[2][0] - mat[0][2]) * s;
            q[3] = (mat[0][1] - mat[1][0]) * s;
        } else {
            if (mat[0][0] > mat[1][1] && mat[0][0] > mat[2][2]) {
                double s = 2.0 * Math.sqrt(1.0 + mat[0][0] - mat[1][1] - mat[2][2]);

                q[1] = 0.25 * s;

                s = 1.0 / s;

                q[0] = (mat[1][2] - mat[2][1]) * s;
                q[2] = (mat[1][0] + mat[0][1]) * s;
                q[3] = (mat[2][0] + mat[0][2]) * s;
            } else if (mat[1][1] > mat[2][2]) {
                double s = 2.0 * Math.sqrt(1.0 + mat[1][1] - mat[0][0] - mat[2][2]);

                q[2] = 0.25 * s;

                s = 1.0 / s;

                q[0] = (mat[2][0] - mat[0][2]) * s;
                q[1] = (mat[1][0] + mat[0][1]) * s;
                q[3] = (mat[2][1] + mat[1][2]) * s;
            } else {
                double s = 2.0 * Math.sqrt(1.0 + mat[2][2] - mat[0][0] - mat[1][1]);

                q[3] = 0.25 * s;

                s = 1.0 / s;

                q[0] = (mat[0][1] - mat[1][0]) * s;
                q[1] = (mat[2][0] + mat[0][2]) * s;
                q[2] = (mat[2][1] + mat[1][2]) * s;
            }
            if (q[0] < 0) {
                for (int i = 0; i < q.length; i++) {
                    q[i] = -q[i];
                }
            }
        }
        return new Quaternion(new double[]{q[1], q[2], q[3], q[0]}).normalized();
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


    @Override
    public String toString() {
        return "Matrix3x3(\n\t[%f, %f, %f],\n\t[%f, %f, %f],\n\t[%f, %f, %f]\n)".formatted(
            matrix[0][0], matrix[0][1], matrix[0][2],
            matrix[1][0], matrix[1][1], matrix[1][2],
            matrix[2][0], matrix[2][1], matrix[2][2]
        );
    }

    public Matrix4x4 to4x4() {
        Matrix4x4 mat = Matrix4x4.Unit();
        System.arraycopy(matrix[0], 0, mat.matrix[0], 0, 3);
        System.arraycopy(matrix[1], 0, mat.matrix[1], 0, 3);
        System.arraycopy(matrix[2], 0, mat.matrix[2], 0, 3);
        return mat;
    }
}
