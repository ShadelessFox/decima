from base64 import b64decode
from collections import defaultdict
from dataclasses import dataclass, asdict, field
from enum import Enum
from pathlib import Path
from typing import Dict, Any, Protocol, runtime_checkable, Tuple, Optional, List

import numpy as np

import numpy.typing as npt


class DMFDataType(Enum):
    DDS = "DDS"
    PNG = "PNG"


class DMFSemantic(Enum):
    POSITION = "POSITION"
    TANGENT = "TANGENT"
    NORMAL = "NORMAL"
    COLOR_0 = "COLOR_0"
    TEXCOORD_0 = "TEXCOORD_0"
    TEXCOORD_1 = "TEXCOORD_1"
    TEXCOORD_2 = "TEXCOORD_2"
    TEXCOORD_3 = "TEXCOORD_3"
    TEXCOORD_4 = "TEXCOORD_4"
    TEXCOORD_5 = "TEXCOORD_5"
    TEXCOORD_6 = "TEXCOORD_6"
    JOINTS_0 = "JOINTS_0"
    JOINTS_1 = "JOINTS_1"
    JOINTS_2 = "JOINTS_2"
    WEIGHTS_0 = "WEIGHTS_0"
    WEIGHTS_1 = "WEIGHTS_1"
    WEIGHTS_2 = "WEIGHTS_2"


class DMFComponentType(Enum):
    SIGNED_SHORT = "SignedShort", np.int16
    SIGNED_SHORT_NORMALIZED = "SignedShortNormalized", np.int16
    UNSIGNED_SHORT_NORMALIZED = "UnsignedShortNormalized", np.uint16
    UNSIGNED_BYTE = "UnsignedByte", np.uint8
    UNSIGNED_BYTE_NORMALIZED = "UnsignedByteNormalized", np.uint8
    FLOAT = "Float", np.float32
    HALF_FLOAT = "HalfFloat", np.float16
    X10Y10Z10W2NORMALIZED = "X10Y10Z10W2Normalized", np.int32

    def __new__(cls, a, b):
        entry = object.__new__(cls)
        entry._value_ = a  # set the value, and the extra attribute
        entry.dtype = b
        return entry


class DMFNodeType(Enum):
    Node = "Node"
    Model = "Model"
    ModelGroup = "ModelGroup"
    LOD = "LOD"


@runtime_checkable
class JsonSerializable(Protocol):

    def to_json(self):
        raise NotImplementedError()

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        raise NotImplementedError()


@dataclass
class DMFTextureDescriptor(JsonSerializable):
    texture_id: int
    channels: str
    usage_type: str

    def to_json(self):
        return {"textureId": self.texture_id, "channels": self.channels, "usageType": self.usage_type}

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["textureId"], data["channels"], data["usageType"])


@dataclass
class DMFSceneMetaData(JsonSerializable):
    generator: str
    version: int

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(**data)


@dataclass
class DMFTexture(JsonSerializable):
    name: str
    data_type: DMFDataType
    buffer_size: int
    usage_type: int
    metadata: Dict[str, str]

    def to_json(self):
        raise NotImplementedError()

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        if "bufferData" in data:
            return DMFInternalTexture.from_json(data)
        else:
            return DMFExternalTexture.from_json(data)


@dataclass
class DMFCollection(JsonSerializable):
    name: str
    enabled: bool = field(default=True)
    parent: Optional[int] = field(default=None)

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], data.get("enabled", True), data.get("parent", None))


@dataclass
class DMFTransform(JsonSerializable):
    position: Tuple[float, float, float]
    scale: Tuple[float, float, float]
    rotation: Tuple[float, float, float, float]

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(**data)


@dataclass
class DMFBone(JsonSerializable):
    name: str
    transform: DMFTransform
    parent_id: int
    local_space: bool

    def to_json(self):
        return {
            "name": self.name,
            "transform": self.transform.to_json(),
            "parentId": self.parent_id,
            "localSpace": self.local_space
        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(
            data["name"],
            DMFTransform.from_json(data["transform"]),
            data["parentId"],
            data.get("localSpace", False)
        )


@dataclass
class DMFBuffer(JsonSerializable):
    original_name: str
    buffer_size: int
    buffer_data: str

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["originalName"], data["bufferSize"], data["bufferData"])

    def to_json(self):
        return {
            "originalName": self.original_name,
            "bufferSize": self.buffer_size,
            "bufferData": self.buffer_data,
        }


@dataclass
class DMFBufferView(JsonSerializable):
    buffer_id: int
    offset: int
    size: int

    def to_json(self):
        return {"bufferId": self.buffer_id, "offset": self.offset, "size": self.size}

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["bufferId"], data["offset"], data["size"])


@dataclass
class DMFSkeleton(JsonSerializable):
    bones: List[DMFBone]
    transform: Optional[DMFTransform]

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        transform = DMFTransform.from_json(data["transform"]) if "transform" in data else None
        return cls([DMFBone.from_json(item) for item in data.get("bones", [])], transform)


@dataclass
class DMFMaterial(JsonSerializable):
    name: str
    type: str
    texture_ids: Dict[str, int] = field(default_factory=dict)
    texture_descriptors: List[DMFTextureDescriptor] = field(default_factory=list)

    def to_json(self):
        return {
            "name": self.name,
            "type": self.type,
            "textureIds": self.texture_ids,
            "textureDescriptors": [desc.to_json() for desc in self.texture_descriptors]
        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], data.get("type", "UNKNOWN"), data.get("textureIds", []),
                   [DMFTextureDescriptor.from_json(desc) for desc in data.get("textureDescriptors", [])])


@dataclass
class DMFNode(JsonSerializable):
    type: DMFNodeType
    name: Optional[str]
    collection_ids: List[int]
    transform: Optional[DMFTransform]
    children: List['DMFNode']
    visible: bool

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        if data is None:
            return None
        node_type = DMFNodeType(data["type"])
        name = data.get("name", node_type.name)
        collection_ids = list(set(data.get('collectionIds', [])))
        transform = DMFTransform.from_json(data["transform"]) if "transform" in data else None
        children = [cls.from_json(item) for item in data.get("children", [])]

        if node_type == DMFNodeType.Model:
            return DMFModel(node_type, name, collection_ids, transform, children, data.get("visible", True),
                            DMFMesh.from_json(data["mesh"]), data.get("skeletonId", None))
        elif node_type == DMFNodeType.ModelGroup:
            return DMFModelGroup(node_type, name, collection_ids, transform, children, data.get("visible", True))
        elif node_type == DMFNodeType.LOD:
            return DMFLodModel(node_type, name, collection_ids, transform, children, data.get("visible", True),
                               [DMFLod.from_json(lod_data) for lod_data in data.get("lods", [])])
        else:
            return DMFNode(node_type, name, collection_ids, transform, children, data.get("visible", True))


@dataclass
class DMFSceneFile(JsonSerializable):
    meta_data: DMFSceneMetaData
    collections: List[DMFCollection]
    models: List[DMFNode]
    skeletons: List[DMFSkeleton]
    buffers: List[DMFBuffer]
    buffer_views: List[DMFBufferView]
    materials: List[DMFMaterial]
    textures: List[DMFTexture]

    _buffers_path: Optional[Path] = field(default=None)

    def to_json(self):
        return {
            "metadata": self.meta_data.to_json(),
            "collections": [item.to_json() for item in self.collections],
            "models": [item.to_json() for item in self.models],
            "buffers": [item.to_json() for item in self.buffers],
            "bufferViews": [item.to_json() for item in self.buffer_views],
            "materials": [item.to_json() for item in self.materials],
            "textures": [item.to_json() for item in self.textures],
        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(
            DMFSceneMetaData.from_json(data["metadata"]),
            [DMFCollection.from_json(item) for item in data.get("collections", [])],
            [DMFNode.from_json(item) for item in data.get("models", [])],
            [DMFSkeleton.from_json(item) for item in data.get("skeletons", [])],
            [DMFBuffer.from_json(item) for item in data.get("buffers", [])],
            [DMFBufferView.from_json(item) for item in data.get("bufferViews", [])],
            [DMFMaterial.from_json(item) for item in data.get("materials", [])],
            [DMFTexture.from_json(item) for item in data.get("textures", [])],
        )

    def set_buffers_path(self, buffers_path: Path):
        self._buffers_path = buffers_path

    @property
    def buffers_path(self):
        assert self._buffers_path
        return self._buffers_path


@dataclass
class DMFInternalTexture(DMFTexture):
    buffer_data: str

    def to_json(self):
        return {
            "name": self.name,
            "bufferData": self.buffer_data,
            "bufferSize": self.buffer_size,
            "dataType": self.data_type,
            "usageType": self.usage_type,
            "metadata": self.metadata

        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], DMFDataType(data["dataType"]), data["bufferSize"],
                   data.get("usageType", 0), data.get("metadata", {}), data["bufferData"])


@dataclass
class DMFExternalTexture(DMFTexture):
    buffer_file_name: str

    def to_json(self):
        return {
            "name": self.name,
            "bufferFileName": self.buffer_file_name,
            "bufferSize": self.buffer_size,
            "dataType": self.data_type,
            "usageType": self.usage_type,
            "metadata": self.metadata

        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], DMFDataType(data["dataType"]), data["bufferSize"],
                   data.get("usageType", 0), data.get("metadata", {}), data["bufferFileName"])


@dataclass
class DMFVertexAttribute(JsonSerializable):
    semantic: DMFSemantic
    element_count: int
    element_type: DMFComponentType
    size: int
    stride: Optional[int]
    offset: Optional[int]
    buffer_view_id: int

    def to_json(self):
        return {
            "semantic": self.semantic,
            "elementCount": self.element_count,
            "elementyType": self.element_type,
            "size": self.size,
            "stride": self.stride,
            "offset": self.offset,
            "bufferViewId": self.buffer_view_id
        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(
            DMFSemantic(data["semantic"]),
            data["elementCount"],
            DMFComponentType(data["elementType"]),
            data["size"],
            data.get("stride", None),
            data.get("offset", 0),
            data["bufferViewId"]
        )

    def convert(self, scene) -> npt.NDArray:
        buffer = scene.buffers_views[self.buffer_view_id].get_data(scene)[self.offset:]
        data = np.frombuffer(buffer, self.element_type.dtype).reshape((-1, self.element_count))
        return data


class VertexType(Enum):
    MULTI_BUFFER = "MULTI_BUFFER"
    SINGLE_BUFFER = "SINGLE_BUFFER"


def _load_buffer_view(buffer_view: DMFBufferView, scene: DMFSceneFile) -> bytes:
    buffer = scene.buffers[buffer_view.buffer_id]
    buffer_data = b64decode(buffer.buffer_data)
    return buffer_data[buffer_view.offset:buffer_view.offset + buffer_view.size]


@dataclass
class DMFPrimitive(JsonSerializable):
    grouping_id: int
    vertex_count: int
    vertex_start: int
    vertex_end: int
    vertex_attributes: Dict[DMFSemantic, DMFVertexAttribute]
    vertex_type: VertexType
    index_count: int
    index_start: int
    index_end: int
    index_size: int
    index_buffer_view_id: int

    material_id: Optional[int]

    _dtype: npt.DTypeLike = None

    def to_json(self):
        return {
            "groupingId": self.grouping_id,
            "vertexCount": self.vertex_count,
            "vertexStart": self.vertex_start,
            "vertexEnd": self.vertex_end,
            "vertexAttributes": {semantic.name: item.to_json() for semantic, item in self.vertex_attributes.items()},
            "vertexType": self.vertex_type,
            "indexCount": self.index_count,
            "indexStart": self.index_start,
            "indexEnd": self.index_end,
            "indexSize": self.index_size,
            "indexBufferViewId": self.index_buffer_view_id,
            "materialId": self.material_id
        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(
            data["groupingId"],
            data["vertexCount"],
            data["vertexStart"],
            data["vertexEnd"],
            {DMFSemantic(name): DMFVertexAttribute.from_json(item) for name, item in
             data.get("vertexAttributes", {}).items()},
            VertexType(data["vertexType"]),
            data["indexCount"],
            data["indexStart"],
            data["indexEnd"],
            data["indexSize"],
            data["indexBufferViewId"],
            data.get("materialId", None),
        )

    def has_attribute(self, semantic: DMFSemantic):
        return semantic in self.vertex_attributes

    def get_vertices(self, scene: DMFSceneFile):
        mode = self.vertex_type
        dtype_fields = []
        dtype_metadata: Dict[str, str] = {}
        for attribute in self.vertex_attributes.values():
            if attribute.element_count > 1:
                dtype_fields.append((attribute.semantic.name, attribute.element_type.dtype, attribute.element_count))
            else:
                dtype_fields.append((attribute.semantic.name, attribute.element_type.dtype))
            dtype_metadata[attribute.semantic.name] = attribute.element_type.name
        dtype = np.dtype(dtype_fields, metadata=dtype_metadata)
        if mode == VertexType.SINGLE_BUFFER:
            data = np.zeros(self.vertex_count, dtype)
            buffer_groups = defaultdict(list)
            for attr in self.vertex_attributes.values():
                buffer_groups[attr.buffer_view_id].append(attr)

            for buffer_view_id, attributes in buffer_groups.items():

                buffer_data = _load_buffer_view(scene.buffer_views[buffer_view_id], scene)

                stream_dtype_fields = []
                stream_dtype_metadata: Dict[str, str] = {}
                sorted_attributes = sorted(attributes, key=lambda a: a.offset)
                for attribute in sorted_attributes:
                    if attribute.element_count > 1:
                        stream_dtype_fields.append(
                            (attribute.semantic.name, attribute.element_type.dtype, attribute.element_count))
                    else:
                        stream_dtype_fields.append((attribute.semantic.name, attribute.element_type.dtype))
                    stream_dtype_metadata[attribute.semantic.name] = attribute.element_type.name
                stream_dtype = np.dtype(stream_dtype_fields, metadata=dtype_metadata)
                stream = np.frombuffer(buffer_data, stream_dtype, self.vertex_count)
                for attribute in attributes:
                    data[attribute.semantic.name] = stream[attribute.semantic.name]
        else:
            data = np.zeros(self.vertex_count, dtype)
            for attribute in self.vertex_attributes.values():
                data[attribute.semantic][:] = self.vertex_attributes[attribute.semantic].convert(scene)[
                                              self.vertex_start:self.vertex_end]
        return data

    def get_indices(self, scene: DMFSceneFile):
        buffer_data = _load_buffer_view(scene.buffer_views[self.index_buffer_view_id], scene)
        dtype = np.uint16 if self.index_size == 2 else np.uint32
        return np.frombuffer(buffer_data, dtype)[self.index_start:self.index_end].reshape((-1, 3))


@dataclass
class DMFMesh(JsonSerializable):
    primitives: List[DMFPrimitive]
    bone_remap_table: Dict[int, int]

    def to_json(self):
        return {
            "primitives": [item.to_json() for item in self.primitives],
            "boneRemapTable": self.bone_remap_table
        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        remap_table = {int(k): v for k, v in data.get("boneRemapTable", {}).items()}

        return cls([DMFPrimitive.from_json(item) for item in data.get("primitives", [])], remap_table)


@dataclass
class DMFModelGroup(DMFNode):
    pass


@dataclass
class DMFModel(DMFNode):
    mesh: DMFMesh
    skeleton_id: int


@dataclass
class DMFLod(JsonSerializable):
    model: DMFNode
    lod_id: int
    distance: float

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(DMFNode.from_json(data.get("model", None)), data["id"], data["distance"])

    def to_json(self):
        return {"model": self.model.to_json() if self.model else None, "id": self.lod_id, "distance": self.distance}


@dataclass
class DMFLodModel(DMFNode):
    lods: List[DMFLod]
