package com.shade.decima.model.viewer.mesh;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.viewer.shader.ModelShaderProgram;
import com.shade.decima.ui.data.ObjectValueController;
import com.shade.decima.ui.data.ValueController;
import com.shade.util.NotNull;

import java.io.IOException;

public class DecimaLodMesh implements LodMesh {
    private final ValueController<RTTIObject> controller;
    private final LodMesh[] lods;
    private int index;

    public DecimaLodMesh(@NotNull ValueController<RTTIObject> controller) {
        this.controller = controller;
        this.lods = new LodMesh[controller.getValue().objs("Meshes").length];
        this.index = 0;
    }

    @Override
    public void load() throws IOException {
        final Project project = controller.getProject();
        final CoreBinary binary = controller.getBinary();
        final RTTIObject[] lods = controller.getValue().objs("Meshes");

        for (int i = 0; i < lods.length; i++) {
            final var lod = lods[i];
            final var object = lod.ref("Mesh").follow(project, binary);
            final var distance = lod.f32("Distance");
            final var mesh = DecimaMesh.create(new ObjectValueController(controller, object.binary(), object.object()));
            mesh.load();

            this.lods[i] = new LodMesh(mesh, distance);
        }
    }

    @Override
    public void draw(@NotNull ModelShaderProgram program) {
        lods[index].mesh.draw(program);
    }

    @Override
    public void dispose() {
        for (LodMesh submesh : lods) {
            submesh.mesh.dispose();
        }
    }

    @Override
    public void setLod(@NotNull Lod lod) {
        for (int i = 0; i < lods.length; i++) {
            if (lods[i] == lod) {
                index = i;
                return;
            }
        }

        throw new IllegalArgumentException("Unexpected lod: " + lod);
    }

    @NotNull
    @Override
    public Lod getLod() {
        return lods[index];
    }

    @NotNull
    @Override
    public Lod[] getLods() {
        return lods;
    }

    private record LodMesh(@NotNull Mesh mesh, float distance) implements Lod {
        @Override
        public float getDistance() {
            return distance;
        }
    }
}
