import json
import random
from collections import defaultdict
from pathlib import Path
from typing import cast, Optional, Dict, List

import bpy
import numpy as np
import numpy.typing as npt
from mathutils import Vector, Quaternion, Matrix

from DMFAddon.dmflib.material import DMFMaterial
from DMFAddon.dmflib.node import DMFModel, DMFNode, DMFNodeType, DMFModelGroup
from DMFAddon.dmflib.primitive import DMFPrimitive
from DMFAddon.dmflib.scene import DMFSceneFile
from DMFAddon.dmflib.skeleton import DMFSkeleton
from DMFAddon.dmflib.vertex_attribute import DMFSemantic, DMFComponentType, DMFVertexAttribute
from DMFAddon.material_utils import create_material, clear_nodes, Nodes, create_node, connect_nodes, create_texture_node


def _convert_quat(quat):
    return quat[3], quat[0], quat[1], quat[2]


def _get_or_create_collection(name, parent: bpy.types.Collection) -> bpy.types.Collection:
    new_collection = (bpy.data.collections.get(name, None) or
                      bpy.data.collections.new(name))
    if new_collection.name not in parent.children:
        parent.children.link(new_collection)
    new_collection.name = name
    return new_collection


def _add_uv(mesh_data: bpy.types.Mesh, uv_name: str, uv_data: npt.NDArray[float]):
    uv_layer = mesh_data.uv_layers.new(name=uv_name)
    uv_layer_data = uv_data.copy()
    uv_layer.data.foreach_set('uv', uv_layer_data.flatten())


def _convert_type_and_size(semantic: DMFSemantic, input_dtype_array: npt.NDArray, output_dtype: npt.DTypeLike,
                           element_start: Optional[int] = None, element_end: Optional[int] = None):
    input_array = input_dtype_array[semantic.name]

    if element_start is None:
        element_start = 0
    if element_end is None:
        element_end = input_array.shape[-1]

    def _convert(source_array):
        input_dtype = source_array.dtype
        meta_type = input_dtype_array.dtype.metadata[semantic.name]
        if meta_type == DMFComponentType.X10Y10Z10W2NORMALIZED.name:
            x = (source_array >> 0 & 1023 ^ 512) - 512
            y = (source_array >> 10 & 1023 ^ 512) - 512
            z = (source_array >> 20 & 1023 ^ 512) - 512
            w = (source_array >> 30 & 1)

            vector_length = np.sqrt(x ** 2 + y ** 2 + z ** 2)
            x = x.astype(np.float32) / vector_length
            y = y.astype(np.float32) / vector_length
            z = z.astype(np.float32) / vector_length
            return np.dstack([x, y, z, w])[0].astype(output_dtype)

        if input_dtype == output_dtype:
            return source_array.copy()

        if output_dtype == np.float32:
            float_array = source_array.copy().astype(np.float32)
            if input_dtype == np.int16:
                return float_array / 0x7FFF
            elif input_dtype == np.uint16:
                return float_array / 0xFFFF
            elif input_dtype == np.uint8:
                return float_array / 0xFF
            elif input_dtype == np.int8:
                return float_array / 0x7F
            elif input_dtype == np.float16:
                return float_array
        raise NotImplementedError(f"Cannot convert {input_dtype} to {output_dtype}")

    return _convert(input_array)[:, element_start:element_end]


def _all_same(items):
    for first in items:
        break
    else:
        return True  # empty case, note all([]) == True
    return all(x == first for x in items)


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
        bone_rot = Quaternion(_convert_quat(bone_rot))
        mat = Matrix.Translation(bone_pos) @ bone_rot.to_matrix().to_4x4()
        # if bl_bone.parent:
        #     bl_bone.matrix = bl_bone.parent.matrix @ mat
        # else:
        bl_bone.matrix = mat

    bpy.ops.object.mode_set(mode='OBJECT')
    return arm_obj


def build_material(material: DMFMaterial, bl_material, scene: DMFSceneFile):
    def _get_texture(key):
        texture_id = material.texture_ids.get(key, None)
        if texture_id is None:
            return None
        texture = scene.textures[texture_id]
        image = bpy.data.images.get(f"{texture.name}.dds", None)
        if image is None:
            print(f"Texture {texture.name}.dds not found")
        return image

    if bl_material.get("LOADED", False):
        return
    bl_material["LOADED"] = True

    bl_material.use_nodes = True
    clear_nodes(bl_material)
    output_node = create_node(bl_material, Nodes.ShaderNodeOutputMaterial)
    bsdf_node = create_node(bl_material, Nodes.ShaderNodeBsdfPrincipled)
    connect_nodes(bl_material, output_node.inputs[0], bsdf_node.outputs[0])

    for semantic in material.texture_ids.keys():
        create_texture_node(bl_material, _get_texture(semantic), semantic)


def import_dmf_model(model: DMFModel, scene: DMFSceneFile, skeleton: Optional[bpy.types.Object] = None):
    primitives = []

    if model.skeleton_id is not None:
        skeleton = import_dmf_skeleton(scene.skeletons[model.skeleton_id], model.name)

    primitive_groups: Dict[int, List[DMFPrimitive]] = defaultdict(list)
    for primitive in model.mesh.primitives:
        primitive_groups[primitive.grouping_id].append(primitive)

    for primitive_group in primitive_groups.values():
        assert _all_same([primitive.index_count for primitive in primitive_group])
        assert _all_same([primitive.vertex_count for primitive in primitive_group])
        assert _all_same([primitive.vertex_start for primitive in primitive_group])
        assert _all_same([primitive.vertex_end for primitive in primitive_group])
        mesh_data = bpy.data.meshes.new(model.name + f"_MESH")
        mesh_obj = bpy.data.objects.new(model.name, mesh_data)
        material_ids = np.zeros(primitive_group[0].index_count // 3, np.int32)
        material_id = 0
        vertex_data = primitive_group[0].get_vertices(scene)
        total_indices: List[npt.NDArray[np.uint32]] = []
        primitive_0 = primitive_group[0]
        for primitive in primitive_group:
            material = scene.materials[primitive.material_id]
            for texture_id in material.texture_ids.values():
                texture = scene.textures[texture_id]
                if bpy.data.images.get(f"{texture.name}.dds", None) is not None:
                    continue
                image = bpy.data.images.new(f"{texture.name}.dds", width=1, height=1)
                texture_data = texture.get_data()
                image.pack(data=texture_data, data_len=len(texture_data))
                image.source = 'FILE'
                image.use_fake_user = True
                image.alpha_mode = 'CHANNEL_PACKED'

            build_material(material, create_material(material.name, mesh_obj), scene)
            material_ids[primitive.index_start // 3:primitive.index_end // 3] = material_id
            material_id += 1

            indices = primitive.get_indices(scene)
            total_indices.append(indices)

        all_indices = np.vstack(total_indices)

        position_data = _convert_type_and_size(DMFSemantic.POSITION, vertex_data, np.float32, 0, 3)
        mesh_data.from_pydata(position_data, [], all_indices)
        mesh_data.update(calc_edges=True, calc_edges_loose=True)

        vertex_indices = np.zeros((len(mesh_data.loops, )), dtype=np.uint32)
        mesh_data.loops.foreach_get('vertex_index', vertex_indices)
        t_vertex_data = vertex_data[vertex_indices]
        if primitive_0.has_attribute(DMFSemantic.TEXCOORD_0):
            _add_uv(mesh_data, "UV0", _convert_type_and_size(DMFSemantic.TEXCOORD_0, t_vertex_data, np.float32))
        if primitive_0.has_attribute(DMFSemantic.TEXCOORD_1):
            _add_uv(mesh_data, "UV1", _convert_type_and_size(DMFSemantic.TEXCOORD_1, t_vertex_data, np.float32))
        if primitive_0.has_attribute(DMFSemantic.TEXCOORD_2):
            _add_uv(mesh_data, "UV2", _convert_type_and_size(DMFSemantic.TEXCOORD_2, t_vertex_data, np.float32))
        if primitive_0.has_attribute(DMFSemantic.TEXCOORD_3):
            _add_uv(mesh_data, "UV3", _convert_type_and_size(DMFSemantic.TEXCOORD_3, t_vertex_data, np.float32))
        if primitive_0.has_attribute(DMFSemantic.TEXCOORD_4):
            _add_uv(mesh_data, "UV4", _convert_type_and_size(DMFSemantic.TEXCOORD_4, t_vertex_data, np.float32))
        if primitive_0.has_attribute(DMFSemantic.TEXCOORD_5):
            _add_uv(mesh_data, "UV5", _convert_type_and_size(DMFSemantic.TEXCOORD_5, t_vertex_data, np.float32))
        if primitive_0.has_attribute(DMFSemantic.TEXCOORD_6):
            _add_uv(mesh_data, "UV6", _convert_type_and_size(DMFSemantic.TEXCOORD_6, t_vertex_data, np.float32))

        if primitive_0.has_attribute(DMFSemantic.COLOR_0):
            vertex_colors = mesh_data.vertex_colors.new(name="COLOR")
            vertex_colors_data = vertex_colors.data
            color_data = _convert_type_and_size(DMFSemantic.COLOR_0, t_vertex_data, np.float32)
            vertex_colors_data.foreach_set('color', color_data.flatten())

        mesh_data.polygons.foreach_set("use_smooth", np.ones(len(mesh_data.polygons), np.uint32))
        mesh_data.use_auto_smooth = True
        if primitive_0.has_attribute(DMFSemantic.NORMAL):
            normal_data = _convert_type_and_size(DMFSemantic.NORMAL, vertex_data, np.float32, element_end=3)
            mesh_data.normals_split_custom_set_from_vertices(normal_data)

        mesh_data.polygons.foreach_set('material_index', material_ids)

        if skeleton:
            vertex_groups = mesh_obj.vertex_groups
            weight_groups = {bone.name: vertex_groups.new(name=bone.name) for bone in
                             skeleton.data.bones}

            elem_count = 0

            for j in range(3):
                if DMFSemantic(f"JOINTS_{j}") not in primitive_0.vertex_attributes:
                    break
                elem_count += 1

            blend_weights = np.zeros((primitive_0.vertex_count, elem_count * 4 + 1), np.float32)
            blend_indices = np.full((primitive_0.vertex_count, elem_count * 4), -1, np.int32)

            for j in range(3):
                if DMFSemantic(f"JOINTS_{j}") not in primitive_0.vertex_attributes:
                    continue
                blend_indices[:, 4 * j:4 * (j + 1)] = vertex_data[f"JOINTS_{j}"].copy()
                weight_semantic = DMFSemantic(f"WEIGHTS_{j}")
                if primitive_0.has_attribute(weight_semantic):
                    weight_data = _convert_type_and_size(weight_semantic, vertex_data, np.float32)
                    blend_weights[:, 1 + 4 * j:1 + 4 * (j + 1)] = weight_data
            for n, (bone_indices, bone_weights) in enumerate(zip(blend_indices, blend_weights)):
                total = bone_weights.sum()
                remaining = 1 - total
                bone_weights[0] = remaining

            for n, (bone_indices, bone_weights) in enumerate(zip(blend_indices, blend_weights)):
                for bone_index, weight in zip(bone_indices, bone_weights):
                    if bone_index == -1 or weight == 0:
                        continue
                    remapped_bone_index = model.bone_remap_table.index(bone_index)
                    bone = skeleton.data.bones[remapped_bone_index]
                    weight_groups[bone.name].add([n], weight, "ADD")
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
        parent = skeleton
    else:
        parent = primitives[0]

    for child in model.children:
        children, _ = import_dmf_node(child, scene, skeleton)
        groupper = bpy.data.objects.new(model.name + "_CHILDREN", None)
        groupper.parent = parent
        bpy.context.scene.collection.objects.link(groupper)
        groupper.parent = skeleton or primitives[0]
        if model.transform:
            groupper.location = model.transform.position
            groupper.rotation_mode = "QUATERNION"
            groupper.rotation_quaternion = _convert_quat(model.transform.rotation)
            groupper.scale = model.transform.scale
        for child_obj in children:
            child_obj.parent = groupper

    if model.transform is not None:
        parent.location = model.transform.position
        parent.rotation_mode = "QUATERNION"
        parent.rotation_quaternion = _convert_quat(model.transform.rotation)
        parent.scale = model.transform.scale
    for primitive in primitives:
        if primitive == parent:
            continue
        primitive.parent = parent
    return primitives, skeleton


def import_dmf_model_group(model_group: DMFModelGroup, scene: DMFSceneFile,
                           skeleton: Optional[bpy.types.Object] = None):
    group_obj = bpy.data.objects.new(model_group.name, None)
    bpy.context.scene.collection.objects.link(group_obj)
    if model_group.transform:
        group_obj.location = model_group.transform.position
        group_obj.rotation_mode = "QUATERNION"
        group_obj.rotation_quaternion = _convert_quat(model_group.transform.rotation)
        group_obj.scale = model_group.transform.scale
    if skeleton:
        group_obj.parent = skeleton
    for child in model_group.children:
        primitives, skeleton = import_dmf_node(child, scene, skeleton)
        for prim in primitives:
            prim.parent = group_obj

    return [group_obj], skeleton


def import_dmf_node(node: DMFNode, scene: DMFSceneFile, skeleton: Optional[bpy.types.Object] = None):
    if node is None:
        return [], None
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
        scene = DMFSceneFile.from_json(json.load(f))
        scene.set_buffers_path(file.parent / 'dbuffers')
        import_dmf(scene)
