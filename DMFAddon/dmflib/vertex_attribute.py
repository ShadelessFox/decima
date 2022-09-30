from dataclasses import dataclass, asdict
from enum import Enum
from typing import Dict, Any, Optional

import numpy as np
import numpy.typing as npt

from .json_serializable_dataclass import JsonSerializable


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


class DMFDataType(Enum):
    SCALAR = "SCALAR", 1
    VEC2 = "VEC2", 2
    VEC3 = "VEC3", 3
    VEC4 = "VEC4", 4
    MAT2 = "MAT2", 4
    MAT3 = "MAT3", 9
    MAT4 = "MAT4", 12

    def __new__(cls, a, b):
        entry = object.__new__(cls)
        entry._value_ = a  # set the value, and the extra attribute
        entry.element_count = b
        return entry


class DMFComponentType(Enum):
    BYTE = "BYTE", np.int8
    UNSIGNED_BYTE = "UNSIGNED_BYTE", np.uint8
    SHORT = "SHORT", np.int16
    UNSIGNED_SHORT = "UNSIGNED_SHORT", np.uint16
    INT = "INT", np.int32
    UNSIGNED_INT = "UNSIGNED_INT", np.uint32
    FLOAT = "FLOAT", np.float32

    def __new__(cls, a, b):
        entry = object.__new__(cls)
        entry._value_ = a  # set the value, and the extra attribute
        entry.dtype = b
        return entry


@dataclass
class DMFVertexAttribute(JsonSerializable):
    semantic: DMFSemantic
    data_type: DMFDataType
    component_type: DMFComponentType
    size: int
    stride: Optional[int]
    buffer_id: int

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(
            DMFSemantic(data["semantic"]),
            DMFDataType(data["dataType"]),
            DMFComponentType(data["componentType"]),
            data["size"],
            data.get("stride", None),
            data["bufferId"]
        )

    def convert(self, scene) -> npt.NDArray:
        buffer = scene.buffers[self.buffer_id].get_data(scene.buffers_path)
        data = np.frombuffer(buffer, self.component_type.dtype).reshape((-1, self.data_type.element_count))
        return data
