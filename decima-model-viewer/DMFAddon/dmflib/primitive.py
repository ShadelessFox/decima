from base64 import b64decode
from collections import defaultdict
from enum import Enum

import numpy as np
import numpy.typing as npt
from dataclasses import dataclass
from typing import Dict, Any, Optional, TYPE_CHECKING

from .buffer import DMFBuffer
from .buffer_view import DMFBufferView
from .json_protocol import JsonSerializable
from .vertex_attribute import DMFVertexAttribute, DMFSemantic

if TYPE_CHECKING:
    from .scene import DMFSceneFile
else:
    class DMFSceneFile:
        ...


class VertexType(Enum):
    MULTI_BUFFER = "MULTI_BUFFER"
    SINGLE_BUFFER = "SINGLE_BUFFER"


def _load_buffer(buffer: DMFBuffer) -> bytes:
    return b64decode(buffer.buffer_data)


def _load_buffer_view(buffer_view: DMFBufferView, scene: DMFSceneFile) -> bytes:
    buffer = scene.buffers[buffer_view.buffer_id]
    buffer_data = _load_buffer(buffer)
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
