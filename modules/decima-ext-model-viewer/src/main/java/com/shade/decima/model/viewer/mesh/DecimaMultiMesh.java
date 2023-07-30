package com.shade.decima.model.viewer.mesh;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.viewer.shader.ModelShaderProgram;
import com.shade.decima.ui.data.ObjectValueController;
import com.shade.decima.ui.data.ValueController;
import com.shade.util.NotNull;
import org.joml.Matrix3f;
import org.joml.Vector3d;

import java.io.IOException;

public class DecimaMultiMesh implements Mesh {
    private final ValueController<RTTIObject> controller;
    private final Mesh[] parts;

    public DecimaMultiMesh(@NotNull ValueController<RTTIObject> controller) {
        this.controller = controller;
        this.parts = new Mesh[controller.getValue().objs("Parts").length];
    }

    @Override
    public void load() throws IOException {
        final Project project = controller.getProject();
        final CoreBinary binary = controller.getBinary();
        final RTTIObject[] parts = controller.getValue().objs("Parts");

        for (int i = 0; i < parts.length; i++) {
            final var part = parts[i];
            final var object = part.ref("Mesh").follow(project, binary);
            final var transform = part.obj("Transform");
            final var position = getVector3d(transform.obj("Position"));
            final var orientation = getMatrix3f(transform.obj("Orientation"));

            this.parts[i] = DecimaMesh.create(new ObjectValueController(controller, object.binary(), object.object()));
            this.parts[i].load();
        }
    }

    @Override
    public void draw(@NotNull ModelShaderProgram program) {
        for (Mesh part : parts) {
            part.draw(program);
        }
    }

    @Override
    public void dispose() {
        for (Mesh part : parts) {
            part.dispose();
        }
    }

    @NotNull
    private static Vector3d getVector3d(@NotNull RTTIObject object) {
        return new Vector3d(
            object.f64("X"),
            object.f64("Y"),
            object.f64("Z")
        );
    }

    @NotNull
    private static Matrix3f getMatrix3f(@NotNull RTTIObject object) {
        final RTTIObject col0 = object.obj("Col0");
        final RTTIObject col1 = object.obj("Col1");
        final RTTIObject col2 = object.obj("Col2");

        return new Matrix3f(
            col0.f32("X"), col0.f32("Y"), col0.f32("Z"),
            col1.f32("X"), col1.f32("Y"), col1.f32("Z"),
            col2.f32("X"), col2.f32("Y"), col2.f32("Z")
        );
    }
}
