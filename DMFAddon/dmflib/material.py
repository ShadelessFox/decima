from dataclasses import dataclass, field
from typing import Dict, Any

from .json_serializable_dataclass import JsonSerializable


@dataclass
class DMFMaterial(JsonSerializable):
    name: str
    roughness: float
    specular: float
    metalnes: float
    texture_ids: Dict[str, int] = field(default_factory=dict)

    def to_json(self):
        return {
            "name": self.name,
            "roughness": self.roughness,
            "specular": self.specular,
            "metalnes": self.metalnes,
            "textureIds": self.texture_ids,
        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], data["roughness"], data["specular"], data["metalnes"], data.get("textureIds", []))
