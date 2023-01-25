from dataclasses import dataclass, asdict
from typing import List, Dict, Any

from .json_protocol import JsonSerializable
from .primitive import DMFPrimitive


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
