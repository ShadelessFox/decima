package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A node in the node hierarchy.
 */
public class Node {
    private static final Matrix4fc IDENTITY_MATRIX = new Matrix4f();

    private final List<Node> children = new ArrayList<>();

    private Matrix4f matrix;
    private Mesh mesh;
    private String name;
    private boolean visible = true;

    @NotNull
    public List<Node> getChildren() {
        return children;
    }

    public void add(@Nullable Node child) {
        if (child != null) {
            children.add(child);
        }
    }

    @Nullable
    public Node accept(@NotNull Visitor visitor) {
        if (visitor.enterNode(this)) {
            final List<Node> children = this.children.stream()
                .map(x -> x.accept(visitor))
                .filter(Objects::nonNull)
                .toList();

            if (children.equals(this.children)) {
                return visitor.leaveNode(this);
            } else {
                final Node node = new Node();
                node.children.addAll(children);
                node.setMatrix(matrix);
                node.setMesh(mesh);
                node.setName(name);
                node.setVisible(visible);
                return visitor.leaveNode(node);
            }
        }

        return this;
    }

    @Nullable
    public Matrix4fc getMatrix() {
        return matrix;
    }

    public void setMatrix(@Nullable Matrix4fc matrix) {
        if (matrix == null || IDENTITY_MATRIX.equals(matrix)) {
            this.matrix = null;
        } else {
            this.matrix = new Matrix4f(matrix);
        }
    }

    @Nullable
    public Mesh getMesh() {
        return mesh;
    }

    public void setMesh(@Nullable Mesh mesh) {
        this.mesh = mesh;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
