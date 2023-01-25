from dataclasses import dataclass, field
from typing import Dict, Any, List

from .json_protocol import JsonSerializable


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
