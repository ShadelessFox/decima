from dataclasses import dataclass
from typing import Dict, Any, Optional

import numpy as np
import numpy.typing as npt

from .json_serializable_dataclass import JsonSerializable
from .vertex_attribute import DMFVertexAttribute, DMFSemantic


@dataclass
class DMFPrimitive(JsonSerializable):
    vertex_count: int
    vertex_start: int
    vertex_end: int
    vertex_attributes: Dict[DMFSemantic, DMFVertexAttribute]

    index_count: int
    index_start: int
    index_end: int
    index_size: int
    index_buffer_id: int

    material_id: Optional[int]

    def to_json(self):
        return {
            "vertexCount": self.vertex_count,
            "vertexStart": self.vertex_start,
            "vertexEnd": self.vertex_end,
            "vertexAttributes": {semantic.name: DMFVertexAttribute.from_json(item) for semantic, item in
                                 self.vertex_attributes.items()},
            "indexCount": self.index_count,
            "indexStart": self.index_start,
            "indexEnd": self.index_end,
            "indexSize": self.index_size,
            "indexBufferId": self.index_buffer_id,
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
            data["indexCount"],
            data["indexStart"],
            data["indexEnd"],
            data["indexSize"],
            data["indexBufferId"],
            data.get("materialId", None),
        )

    def get_attribute(self, semantic: DMFSemantic, scene) -> npt.NDArray:
        assert semantic in self.vertex_attributes, f"Semantic {semantic} not found in Primitive vertex attributes"
        return self.vertex_attributes[semantic].convert(scene)[self.vertex_start:self.vertex_end]

    def get_indices(self, scene):
        buffer = scene.buffers[self.index_buffer_id].get_data(scene.buffers_path)
        dtype = np.uint16 if self.index_size == 2 else np.uint32
        return np.frombuffer(buffer, dtype).reshape((-1, 3))[self.index_start:self.index_end]
