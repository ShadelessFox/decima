from dataclasses import dataclass, field
from typing import Dict, Any, List

from .json_serializable_dataclass import JsonSerializable
from .texture_descriptor import DMFTextureDescriptor


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
        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], data.get("type", "UNKNOWN"), data.get("textureIds", []),
                   [DMFTextureDescriptor.from_json(desc) for desc in data.get("textureDescriptors", [])])
