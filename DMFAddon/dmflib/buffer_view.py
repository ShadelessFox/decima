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

    def get_data(self, scene):
        buffer = scene.buffers[self.buffer_id]
        buffer_data = buffer.get_data(scene.buffers_path)
        return buffer_data[self.offset:self.offset + self.size]
