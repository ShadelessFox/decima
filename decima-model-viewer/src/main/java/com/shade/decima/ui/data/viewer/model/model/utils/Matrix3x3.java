package com.shade.decima.ui.data.viewer.model.model.utils;

import com.shade.util.NotNull;

public record Matrix3x3(@NotNull double[][] matrix) {
    public Matrix3x3(@NotNull double[][] matrix) {
        this.matrix = new double[3][3];
        System.arraycopy(matrix[0], 0, this.matrix[0], 0, 3);
        System.arraycopy(matrix[1], 0, this.matrix[1], 0, 3);
        System.arraycopy(matrix[2], 0, this.matrix[2], 0, 3);
    }

    @NotNull
    public static Matrix3x3 identity() {
        return new Matrix3x3(new double[][]{
            {1, 0, 0},
            {0, 1, 0},
            {0, 0, 1},
        });
    }

    @NotNull
    public Matrix3x3 negative() {
        final Matrix3x3 output = Matrix3x3.identity();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                output.matrix[i][j] = -matrix[i][j];
            }
        }
        return output;
    }

    public double determinant() {
        return determinant(matrix);
    }

    public static double determinant(double[][] matrix) {
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
            mat = Matrix3x3.identity().matrix;
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

    @NotNull
    public Matrix4x4 to4x4() {
        Matrix4x4 mat = Matrix4x4.identity();
        System.arraycopy(matrix[0], 0, mat.matrix()[0], 0, 3);
        System.arraycopy(matrix[1], 0, mat.matrix()[1], 0, 3);
        System.arraycopy(matrix[2], 0, mat.matrix()[2], 0, 3);
        return mat;
    }
}
