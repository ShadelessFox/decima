package com.shade.decima.model.viewer.mesh;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.viewer.gl.ShaderProgram;
import com.shade.decima.ui.data.ObjectValueController;
import com.shade.decima.ui.data.ValueController;
import com.shade.util.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DecimaMultiMesh implements Mesh {
    private final ValueController<RTTIObject> controller;
    private final List<Mesh> meshes;
    private final List<Matrix4f> transforms;

    public DecimaMultiMesh(@NotNull ValueController<RTTIObject> controller) {
        this.controller = controller;
        this.meshes = new ArrayList<>();
        this.transforms = new ArrayList<>();
    }

    @Override
    public void load() throws IOException {
        final Project project = controller.getProject();
        final CoreBinary binary = controller.getBinary();
        final RTTIObject object = controller.getValue();

        if (project.getContainer().getType() == GameType.DSDC) {
            final RTTIReference[] meshes = object.refs("Meshes");
            final RTTIObject[] transforms = object.objs("Transforms");

            for (int i = 0; i < meshes.length; i++) {
                final var meshResult = meshes[i].follow(project, binary);
                final var mesh = DecimaMesh.create(new ObjectValueController(controller, meshResult.binary(), meshResult.object()));
                mesh.load();

                this.meshes.add(mesh);
                this.transforms.add(transforms.length == 0 ? new Matrix4f().identity() : getMat34(transforms[i]));
            }
        } else {
            for (final RTTIObject part : object.objs("Parts")) {
                final var meshResult = part.ref("Mesh").follow(project, binary);
                final var mesh = DecimaMesh.create(new ObjectValueController(controller, meshResult.binary(), meshResult.object()));
                mesh.load();

                this.meshes.add(mesh);
                this.transforms.add(getWorldTransform(part.obj("Transform")));
            }
        }
    }

    @Override
    public void draw(@NotNull ShaderProgram program, @NotNull Matrix4fc transform) {
        for (int i = 0; i < meshes.size(); i++) {
            meshes.get(i).draw(program, transform.mul(transforms.get(i), new Matrix4f()));
        }
    }

    @Override
    public void dispose() {
        for (Mesh part : meshes) {
            part.dispose();
        }
    }

    @NotNull
    private static Matrix4f getWorldTransform(@NotNull RTTIObject object) {
        final RTTIObject col0 = object.obj("Orientation").obj("Col0");
        final RTTIObject col1 = object.obj("Orientation").obj("Col1");
        final RTTIObject col2 = object.obj("Orientation").obj("Col2");
        final RTTIObject pos = object.obj("Position");

        return new Matrix4f(
            col0.f32("X"), col0.f32("Y"), col0.f32("Z"), 0,
            col1.f32("X"), col1.f32("Y"), col1.f32("Z"), 0,
            col2.f32("X"), col2.f32("Y"), col2.f32("Z"), 0,
            (float) pos.f64("X"), (float) pos.f64("Y"), (float) pos.f64("Z"), 1
        );
    }

    @NotNull
    private static Matrix4f getMat34(@NotNull RTTIObject object) {
        final RTTIObject row0 = object.obj("Row0");
        final RTTIObject row1 = object.obj("Row1");
        final RTTIObject row2 = object.obj("Row2");

        return new Matrix4f(
            row0.f32("X"), row1.f32("X"), row2.f32("X"), 0,
            row0.f32("Y"), row1.f32("Y"), row2.f32("Y"), 0,
            row0.f32("Z"), row1.f32("Z"), row2.f32("Z"), 0,
            row0.f32("W"), row1.f32("W"), row2.f32("W"), 1
        );
    }
}
