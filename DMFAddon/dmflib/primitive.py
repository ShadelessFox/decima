from collections import defaultdict
from enum import Enum

import numpy as np
import numpy.typing as npt
from dataclasses import dataclass
from typing import Dict, Any, Optional
from itertools import groupby

from .json_serializable_dataclass import JsonSerializable
from .vertex_attribute import DMFVertexAttribute, DMFSemantic


class VertexType(Enum):
    MULTI_BUFFER = "MULTIBUFFER"
    SINGLE_BUFFER = "SINGLEBUFFER"


@dataclass
class DMFPrimitive(JsonSerializable):
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
            "vertexCount": self.vertex_count,
            "vertexStart": self.vertex_start,
            "vertexEnd": self.vertex_end,
            "vertexAttributes": {semantic.name: DMFVertexAttribute.from_json(item) for semantic, item in
                                 self.vertex_attributes.items()},
            "vertex_type": self.vertex_type,
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

    def get_vertices(self, scene):
        mode = self.vertex_type
        dtype_fields = []
        for attribute in self.vertex_attributes.values():
            dtype_fields.append(
                (attribute.semantic.name, attribute.component_type.dtype, attribute.data_type.element_count))
        dtype = np.dtype(dtype_fields)
        if mode == VertexType.SINGLE_BUFFER:
            data = np.zeros(self.vertex_count, dtype)
            buffer_groups = defaultdict(list)
            [buffer_groups[attr.buffer_view_id].append(attr) for attr in self.vertex_attributes.values()]

            for buffer_view_id, attributes in buffer_groups.items():
                buffer_data = scene.buffer_views[buffer_view_id].get_data(scene)
                stream_dtype_fields = []
                for attribute in sorted(attributes, key=lambda a: a.offset):
                    stream_dtype_fields.append(
                        (attribute.semantic.name, attribute.component_type.dtype, attribute.data_type.element_count))
                stream_dtype = np.dtype(stream_dtype_fields)
                stream = np.frombuffer(buffer_data, stream_dtype, self.vertex_count)
                for attribute in attributes:
                    data[attribute.semantic.name] = stream[attribute.semantic.name]
        else:
            data = np.zeros(self.vertex_count, dtype)
            for attribute in self.vertex_attributes.values():
                data[attribute.semantic][:] = self.vertex_attributes[attribute.semantic].convert(scene)[
                                              self.vertex_start:self.vertex_end]
        return data

    def get_indices(self, scene):
        buffer = scene.buffer_views[self.index_buffer_view_id].get_data(scene)
        dtype = np.uint16 if self.index_size == 2 else np.uint32
        return np.frombuffer(buffer, dtype).reshape((-1, 3))[self.index_start:self.index_end]
