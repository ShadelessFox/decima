package com.shade.decima.ui.data.viewer.model.model.utils;


import com.shade.util.NotNull;

public record Matrix4x4(@NotNull double[][] matrix) {
    public Matrix4x4(@NotNull double[][] matrix) {
        this.matrix = new double[4][4];
        System.arraycopy(matrix[0], 0, this.matrix[0], 0, 4);
        System.arraycopy(matrix[1], 0, this.matrix[1], 0, 4);
        System.arraycopy(matrix[2], 0, this.matrix[2], 0, 4);
        System.arraycopy(matrix[3], 0, this.matrix[3], 0, 4);
    }

    public Matrix4x4() {
        this(new double[4][4]);
    }

    @NotNull
    public static Matrix4x4 identity() {
        return new Matrix4x4(new double[][]{
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1}
        });
    }

    @NotNull
    public static Matrix4x4 translation(Vector3 translation) {
        Matrix4x4 matrix = Matrix4x4.identity();
        matrix.matrix[0][3] = translation.x();
        matrix.matrix[1][3] = translation.y();
        matrix.matrix[2][3] = translation.z();
        return matrix;
    }

    public Matrix4x4 matMul(Matrix4x4 other) {
        Matrix4x4 otherT = other.transposed();
        Matrix4x4 mat = new Matrix4x4();
        for (int col = 0; col < matrix.length; col++) {
            for (int row = 0; row < matrix.length; row++) {
                double dot = MathUtils.dotProduct(matrix[row], otherT.matrix[col]);
                mat.matrix[col][row] = dot;
            }
        }
        return mat.transposed();
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
        Matrix4x4 res = Matrix4x4.identity();
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

        dstMat[0][0] = Matrix3x3.determinant(new double[][]{{b2, b3, b4}, {c2, c3, c4}, {d2, d3, d4}});
        dstMat[1][0] = -Matrix3x3.determinant(new double[][]{{a2, a3, a4}, {c2, c3, c4}, {d2, d3, d4}});
        dstMat[2][0] = Matrix3x3.determinant(new double[][]{{a2, a3, a4}, {b2, b3, b4}, {d2, d3, d4}});
        dstMat[3][0] = -Matrix3x3.determinant(new double[][]{{a2, a3, a4}, {b2, b3, b4}, {c2, c3, c4}});

        dstMat[0][1] = -Matrix3x3.determinant(new double[][]{{b1, b3, b4}, {c1, c3, c4}, {d1, d3, d4}});
        dstMat[1][1] = Matrix3x3.determinant(new double[][]{{a1, a3, a4}, {c1, c3, c4}, {d1, d3, d4}});
        dstMat[2][1] = -Matrix3x3.determinant(new double[][]{{a1, a3, a4}, {b1, b3, b4}, {d1, d3, d4}});
        dstMat[3][1] = Matrix3x3.determinant(new double[][]{{a1, a3, a4}, {b1, b3, b4}, {c1, c3, c4}});

        dstMat[0][2] = Matrix3x3.determinant(new double[][]{{b1, b2, b4}, {c1, c2, c4}, {d1, d2, d4}});
        dstMat[1][2] = -Matrix3x3.determinant(new double[][]{{a1, a2, a4}, {c1, c2, c4}, {d1, d2, d4}});
        dstMat[2][2] = Matrix3x3.determinant(new double[][]{{a1, a2, a4}, {b1, b2, b4}, {d1, d2, d4}});
        dstMat[3][2] = -Matrix3x3.determinant(new double[][]{{a1, a2, a4}, {b1, b2, b4}, {c1, c2, c4}});

        dstMat[0][3] = -Matrix3x3.determinant(new double[][]{{b1, b2, b3}, {c1, c2, c3}, {d1, d2, d3}});
        dstMat[1][3] = Matrix3x3.determinant(new double[][]{{a1, a2, a3}, {c1, c2, c3}, {d1, d2, d3}});
        dstMat[2][3] = -Matrix3x3.determinant(new double[][]{{a1, a2, a3}, {b1, b2, b3}, {d1, d2, d3}});
        dstMat[3][3] = Matrix3x3.determinant(new double[][]{{a1, a2, a3}, {b1, b2, b3}, {c1, c2, c3}});
        return res;
    }

    public double determinant() {
        double determinant1 = Matrix3x3.determinant(new double[][]{
                {matrix[1][1], matrix[1][2], matrix[1][3]},
                {matrix[2][1], matrix[2][2], matrix[2][3]},
                {matrix[3][1], matrix[3][2], matrix[3][3]},
            }
        );
        double determinant2 = Matrix3x3.determinant(new double[][]{
                {matrix[1][0], matrix[1][2], matrix[1][3]},
                {matrix[2][0], matrix[2][2], matrix[2][3]},
                {matrix[3][0], matrix[3][2], matrix[3][3]},
            }
        );
        double determinant3 = Matrix3x3.determinant(new double[][]{
                {matrix[1][0], matrix[1][1], matrix[1][3]},
                {matrix[2][0], matrix[2][1], matrix[2][3]},
                {matrix[3][0], matrix[3][1], matrix[3][3]},
            }
        );
        double determinant4 = Matrix3x3.determinant(new double[][]{
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

    @NotNull
    public Quaternion toQuaternion() {
        return new Matrix3x3(matrix).toQuaternion();
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
    public Vector3 toTranslation() {
        return new Vector3(matrix[0][3], matrix[1][3], matrix[2][3]);
    }

    public boolean isNegative() {
        return determinant() < 0;
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
                invertedMatrix = Matrix4x4.identity();
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
