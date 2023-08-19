package com.shade.decima.model.viewer.mesh;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.viewer.gl.ShaderProgram;
import com.shade.decima.ui.data.ObjectValueController;
import com.shade.decima.ui.data.ValueController;
import com.shade.util.NotNull;
import org.joml.Matrix4fc;

import java.io.IOException;

public class DecimaArtPartMesh implements Mesh {
    private final ValueController<RTTIObject> controller;

    private Mesh mesh;
    private Mesh[] children;

    public DecimaArtPartMesh(@NotNull ValueController<RTTIObject> controller) {
        this.controller = controller;
    }

    @Override
    public void load() throws IOException {
        final RTTIObject object = controller.getValue();
        final Project project = controller.getProject();
        final CoreBinary binary = controller.getBinary();

        final boolean subModel = object.type().isInstanceOf("ArtPartsSubModelWithChildrenResource");

        final RTTIReference.FollowResult rootModel = object.ref(subModel ? "ArtPartsSubModelPartResource" : "RootModel").follow(project, binary);
        final RTTIReference.FollowResult rootModelMesh = rootModel != null ? rootModel.object().ref("MeshResource").follow(project, rootModel.binary()) : null;

        if (rootModelMesh != null) {
            mesh = DecimaMesh.create(new ObjectValueController(controller, rootModelMesh.binary(), rootModelMesh.object()));
            mesh.load();
        }

        final RTTIReference[] parts = object.refs(subModel ? "Children" : "SubModelPartResources");
        children = new Mesh[parts.length];

        for (int i = 0; i < parts.length; i++) {
            final RTTIReference subModelPartResourceRef = parts[i];
            final RTTIReference.FollowResult subModelPartResource = subModelPartResourceRef.follow(project, binary);

            children[i] = DecimaMesh.create(new ObjectValueController(controller, subModelPartResource.binary(), subModelPartResource.object()));
            children[i].load();
        }
    }

    @Override
    public void draw(@NotNull ShaderProgram program, @NotNull Matrix4fc transform) {
        if (mesh != null) {
            mesh.draw(program, transform);
        }

        for (Mesh child : children) {
            child.draw(program, transform);
        }
    }

    @Override
    public void dispose() {
        if (mesh != null) {
            mesh.dispose();
        }

        for (Mesh child : children) {
            child.dispose();
        }
    }
}
