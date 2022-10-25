from dataclasses import dataclass
from typing import Dict, Any

from .json_serializable_dataclass import JsonSerializable


@dataclass
class DMFBufferView(JsonSerializable):
    buffer_id: int
    offset: int
    size: int

    def to_json(self):
        return {"bufferId": self.buffer_id, "offset": self.offset, "size": self.size}

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["bufferId"], data["offset"], data["size"])
