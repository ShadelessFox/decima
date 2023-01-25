from dataclasses import dataclass, asdict
from typing import Dict, Any

from .json_serializable_dataclass import JsonSerializable

from .transform import DMFTransform


@dataclass
class DMFBone(JsonSerializable):
    name: str
    transform: DMFTransform
    parent_id: int
    local_space: bool

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], DMFTransform.from_json(data["transform"]), data["parentId"],
                   data.get("localSpace", False))
