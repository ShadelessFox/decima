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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DecimaMultiMesh implements Mesh {
    private final ValueController<RTTIObject> controller;
    private final List<Mesh> parts;

    public DecimaMultiMesh(@NotNull ValueController<RTTIObject> controller) {
        this.controller = controller;
        this.parts = new ArrayList<>();
    }

    @Override
    public void load() throws IOException {
        final Project project = controller.getProject();
        final CoreBinary binary = controller.getBinary();
        final RTTIObject object = controller.getValue();

        if (project.getContainer().getType() == GameType.DSDC) {
            final RTTIReference[] meshes = object.refs("Meshes");

            for (RTTIReference ref : meshes) {
                final var meshResult = ref.follow(project, binary);
                final var mesh = DecimaMesh.create(new ObjectValueController(controller, meshResult.binary(), meshResult.object()));
                mesh.load();

                parts.add(mesh);
            }
        } else {
            for (final RTTIObject part : object.objs("Parts")) {
                final var meshResult = part.ref("Mesh").follow(project, binary);
                final var mesh = DecimaMesh.create(new ObjectValueController(controller, meshResult.binary(), meshResult.object()));
                mesh.load();

                parts.add(mesh);
            }
        }
    }

    @Override
    public void draw(@NotNull ShaderProgram program) {
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
}
