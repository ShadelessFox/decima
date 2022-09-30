import json
from pathlib import Path
from typing import cast, Optional

import bpy
import numpy as np
import numpy.typing as npt
from mathutils import Vector, Quaternion, Matrix

from DMFAddon.dmflib.node import DMFModel, DMFNode, DMFNodeType, DMFModelGroup
from DMFAddon.dmflib.scene import DMFSceneFile
from DMFAddon.dmflib.skeleton import DMFSkeleton
from DMFAddon.dmflib.vertex_attribute import DMFSemantic


def get_or_create_collection(name, parent: bpy.types.Collection) -> bpy.types.Collection:
    new_collection = (bpy.data.collections.get(name, None) or
                      bpy.data.collections.new(name))
    if new_collection.name not in parent.children:
        parent.children.link(new_collection)
    new_collection.name = name
    return new_collection


def _add_uv(mesh_data: bpy.types.Mesh, uv_name: str, uv_data: npt.NDArray[float], vertex_indices: npt.NDArray[int]):
    uv_layer = mesh_data.uv_layers.new(name=uv_name)
    uv_layer_data = uv_data.copy()[vertex_indices]
    uv_layer_data[:, 1] = 1 - uv_layer_data[:, 1]
    uv_layer.data.foreach_set('uv', uv_layer_data.flatten().astype(np.float32))


def import_dmf_skeleton(skeleton: DMFSkeleton, name: str):
    arm_data = bpy.data.armatures.new(name + "_ARMDATA")
    arm_obj = bpy.data.objects.new(name + "_ARM", arm_data)
    bpy.context.scene.collection.objects.link(arm_obj)

    arm_obj.show_in_front = True
    arm_obj.select_set(True)
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.mode_set(mode='EDIT')

    bones = []
    for bone in skeleton.bones:
        bl_bone = arm_data.edit_bones.new(bone.name)
        bl_bone.tail = Vector([0, 0, 0.1]) + bl_bone.head
        bones.append(bl_bone)

        if bone.parent_id != -1:
            bl_bone.parent = bones[bone.parent_id]

        bone_pos = bone.transform.position
        bone_rot = bone.transform.rotation

        bone_pos = Vector([bone_pos[0], bone_pos[1], bone_pos[2]])
        # noinspection PyTypeChecker
        bone_rot = Quaternion([bone_rot[3], bone_rot[0], bone_rot[1], bone_rot[2]])
        mat = Matrix.Translation(bone_pos) @ bone_rot.to_matrix().to_4x4()
        if bl_bone.parent:
            bl_bone.matrix = bl_bone.parent.matrix @ mat
        else:
            bl_bone.matrix = mat

    bpy.ops.object.mode_set(mode='OBJECT')
    return arm_obj


def import_dmf_model(model: DMFModel, scene: DMFSceneFile, skeleton: Optional[bpy.types.Object] = None):
    if model.skeleton_id is not None and skeleton is not None:
        raise ValueError("Something went wrong")
    primitives = []

    if model.skeleton_id is not None:
        skeleton = import_dmf_skeleton(scene.skeletons[model.skeleton_id], model.name)

    for i, primitive in enumerate(model.mesh.primitives):
        mesh_data = bpy.data.meshes.new(model.name + f"_PRIM_{i}_MESH")
        mesh_obj = bpy.data.objects.new(model.name, mesh_data)

        pos = primitive.get_attribute(DMFSemantic.POSITION, scene)
        indices = primitive.get_indices(scene)
        assert indices.max() < len(pos)
        mesh_data.from_pydata(pos, [], indices)
        mesh_data.update()

        vertex_indices = np.zeros((len(mesh_data.loops, )), dtype=np.uint32)
        mesh_data.loops.foreach_get('vertex_index', vertex_indices)
        if DMFSemantic.TEXCOORD_0 in primitive.vertex_attributes:
            _add_uv(mesh_data, "UV0", primitive.get_attribute(DMFSemantic.TEXCOORD_0, scene), vertex_indices)
        if DMFSemantic.TEXCOORD_1 in primitive.vertex_attributes:
            _add_uv(mesh_data, "UV1", primitive.get_attribute(DMFSemantic.TEXCOORD_1, scene), vertex_indices)
        if DMFSemantic.TEXCOORD_2 in primitive.vertex_attributes:
            _add_uv(mesh_data, "UV2", primitive.get_attribute(DMFSemantic.TEXCOORD_2, scene), vertex_indices)
        if DMFSemantic.TEXCOORD_3 in primitive.vertex_attributes:
            _add_uv(mesh_data, "UV3", primitive.get_attribute(DMFSemantic.TEXCOORD_3, scene), vertex_indices)
        if DMFSemantic.TEXCOORD_4 in primitive.vertex_attributes:
            _add_uv(mesh_data, "UV4", primitive.get_attribute(DMFSemantic.TEXCOORD_4, scene), vertex_indices)
        if DMFSemantic.TEXCOORD_5 in primitive.vertex_attributes:
            _add_uv(mesh_data, "UV5", primitive.get_attribute(DMFSemantic.TEXCOORD_5, scene), vertex_indices)
        if DMFSemantic.TEXCOORD_6 in primitive.vertex_attributes:
            _add_uv(mesh_data, "UV6", primitive.get_attribute(DMFSemantic.TEXCOORD_6, scene), vertex_indices)

        if DMFSemantic.COLOR_0 in primitive.vertex_attributes:
            vertex_colors = mesh_data.vertex_colors.new(name="COLOR")
            vertex_colors_data = vertex_colors.data
            vertex_colors_data.foreach_set('color', primitive.get_attribute(DMFSemantic.COLOR_0, scene)[
                vertex_indices].flatten())

        mesh_data.polygons.foreach_set("use_smooth", np.ones(len(mesh_data.polygons), np.uint32))
        mesh_data.use_auto_smooth = True
        if DMFSemantic.NORMAL in primitive.vertex_attributes:
            mesh_data.normals_split_custom_set_from_vertices(primitive.get_attribute(DMFSemantic.NORMAL, scene))

        if skeleton:
            vertex_groups = mesh_obj.vertex_groups
            weight_groups = {bone.name: vertex_groups.new(name=bone.name) for bone in
                             skeleton.data.bones}
            for j in range(3):
                if DMFSemantic(f"JOINTS_{j}") not in primitive.vertex_attributes:
                    continue
                print(f"Processing JOINTS_{j}")
                blend_indices = primitive.get_attribute(DMFSemantic(f"JOINTS_{j}"), scene).copy()
                weight_semantic = DMFSemantic(f"WEIGHTS_{j}")
                if weight_semantic in primitive.vertex_attributes:
                    blend_weights = primitive.get_attribute(weight_semantic, scene).copy()
                    for n, (bone_indices, bone_weights) in enumerate(zip(blend_indices, blend_weights)):
                        if 1.0 > bone_weights.sum() > 0:
                            total = bone_weights.sum()
                            remaining = 1 - total
                            for k in range(4):
                                if bone_weights[k] == 0.0:
                                    bone_weights[k] = remaining
                                    break
                        for bone_index, weight in zip(bone_indices, bone_weights):
                            bone = skeleton.data.bones[bone_index]
                            weight_groups[bone.name].add([n], weight, "ADD")
                    del blend_weights
                else:
                    raise RuntimeError()
                # else:
                #     for n, bone_indices in enumerate(blend_indices):
                #         for bone_index in bone_indices:
                #             bone = skeleton.data.bones[bone_index]
                #             weight_groups[bone.name].add([n], 1, "REPLACE")
        primitives.append(mesh_obj)
        bpy.context.scene.collection.objects.link(mesh_obj)

    if skeleton is not None:
        for primitive in primitives:
            modifier = primitive.modifiers.new(type="ARMATURE", name="Armature")
            modifier.object = skeleton

    if model.skeleton_id is not None:
        for primitive in primitives:
            primitive.parent = skeleton

    for child in model.children:
        children, _ = import_dmf_node(child, scene, skeleton)
        groupper = bpy.data.objects.new(model.name + "_CHILDREN", None)
        groupper.parent = skeleton or primitives[0]
        for child_obj in children:
            child_obj.parent = groupper
    return primitives, skeleton


def import_dmf_model_group(model_group: DMFModelGroup, scene: DMFSceneFile,
                           skeleton: Optional[bpy.types.Object] = None):
    group_obj = bpy.data.objects.new(model_group.name, None)
    if skeleton:
        group_obj.parent = skeleton
    for child in model_group.children:
        primitives, skeleton = import_dmf_node(child, scene, skeleton)

    return [group_obj], skeleton


def import_dmf_node(node: DMFNode, scene: DMFSceneFile, skeleton: Optional[bpy.types.Object] = None):
    if node.type == DMFNodeType.Model:
        return import_dmf_model(cast(DMFModel, node), scene, skeleton)
    elif node.type == DMFNodeType.ModelGroup:
        return import_dmf_model_group(cast(DMFModelGroup, node), scene, skeleton)


def import_dmf(scene: DMFSceneFile):
    if scene.meta_data.version != 1:
        raise ValueError(f"Version {scene.meta_data.version} is not supported!")

    for node in scene.models:
        import_dmf_node(node, scene)


def import_dmf_from_path(file: Path):
    with file.open('r') as f:
        import_dmf(DMFSceneFile.from_json(json.load(f)))
