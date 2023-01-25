from dataclasses import dataclass
from typing import Dict, Any

from .json_protocol import JsonSerializable

from .transform import DMFTransform


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
