package com.shade.decima.scene;

import com.shade.decima.geometry.Mesh;
import com.shade.decima.math.Mat4;
import com.shade.util.Nullable;

import java.util.List;

public record Node(@Nullable String name, @Nullable Mesh mesh, List<Node> children, Mat4 matrix) {
    public Node {
        children = List.copyOf(children);
    }

    public Node transform(Mat4 transform) {
        return new Node(name, mesh, children, matrix.mul(transform));
    }
}
