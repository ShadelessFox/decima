package com.shade.decima.model.exporter.utils;

import com.shade.util.NotNull;

import java.util.Arrays;

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

    public Matrix3x3 normalized() {
        Matrix3x3 output = Matrix3x3.identity();
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

    @NotNull
    public Matrix3x3 transposed() {
        double[][] transposed = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return new Matrix3x3(transposed);
    }

    public Quaternion toQuaternion() {
        Matrix3x3 normalized = normalized();
        double[][] mat = normalized.matrix;
        double det = determinant();
        if (Double.isInfinite(det)) {
            mat = Matrix3x3.identity().matrix;
        } else if (isNegative()) {
            mat = negative().matrix;
        }
        double[] q = new double[4];

        if (mat[2][2] < 0.0f) {
            if (mat[0][0] > mat[1][1]) {
                double trace = 1.0f + mat[0][0] - mat[1][1] - mat[2][2];
                double s = 2.0f * Math.sqrt(trace);
                if (mat[1][2] < mat[2][1]) {
                    /* Ensure W is non-negative for a canonical result. */
                    s = -s;
                }
                q[1] = 0.25f * s;
                s = 1.0f / s;
                q[0] = (mat[1][2] - mat[2][1]) * s;
                q[2] = (mat[0][1] + mat[1][0]) * s;
                q[3] = (mat[2][0] + mat[0][2]) * s;
                if ((trace == 1.0f) && (q[0] == 0.0f && q[2] == 0.0f && q[3] == 0.0f)) {
                    /* Avoids the need to normalize the degenerate case. */
                    q[1] = 1.0f;
                }
            } else {
                double trace = 1.0f - mat[0][0] + mat[1][1] - mat[2][2];
                double s = 2.0f * Math.sqrt(trace);
                if (mat[2][0] < mat[0][2]) {
                    /* Ensure W is non-negative for a canonical result. */
                    s = -s;
                }
                q[2] = 0.25f * s;
                s = 1.0f / s;
                q[0] = (mat[2][0] - mat[0][2]) * s;
                q[1] = (mat[0][1] + mat[1][0]) * s;
                q[3] = (mat[1][2] + mat[2][1]) * s;
                if ((trace == 1.0f) && (q[0] == 0.0f && q[1] == 0.0f && q[3] == 0.0f)) {
                    /* Avoids the need to normalize the degenerate case. */
                    q[2] = 1.0f;
                }
            }
        } else {
            if (mat[0][0] < -mat[1][1]) {
                double trace = 1.0f - mat[0][0] - mat[1][1] + mat[2][2];
                double s = 2.0f * Math.sqrt(trace);
                if (mat[0][1] < mat[1][0]) {
                    /* Ensure W is non-negative for a canonical result. */
                    s = -s;
                }
                q[3] = 0.25f * s;
                s = 1.0f / s;
                q[0] = (mat[0][1] - mat[1][0]) * s;
                q[1] = (mat[2][0] + mat[0][2]) * s;
                q[2] = (mat[1][2] + mat[2][1]) * s;
                if ((trace == 1.0f) && (q[0] == 0.0f && q[1] == 0.0f && q[2] == 0.0f)) {
                    /* Avoids the need to normalize the degenerate case. */
                    q[3] = 1.0f;
                }
            } else {
                /* NOTE(@campbellbarton): A zero matrix will fall through to this block,
                 * needed so a zero scaled matrices to return a quaternion without rotation, see: T101848. */
                double trace = 1.0f + mat[0][0] + mat[1][1] + mat[2][2];
                double s = 2.0f * Math.sqrt(trace);
                q[0] = 0.25f * s;
                s = 1.0f / s;
                q[1] = (mat[1][2] - mat[2][1]) * s;
                q[2] = (mat[2][0] - mat[0][2]) * s;
                q[3] = (mat[0][1] - mat[1][0]) * s;
                if ((trace == 1.0f) && (q[1] == 0.0f && q[2] == 0.0f && q[3] == 0.0f)) {
                    /* Avoids the need to normalize the degenerate case. */
                    q[0] = 1.0f;
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

    @Override
    public String toString() {
        return "Matrix3x3[\n\t%s,\n\t%s,\n\t%s\n]".formatted(Arrays.toString(matrix[0]), Arrays.toString(matrix[1]), Arrays.toString(matrix[2]));
    }
}
