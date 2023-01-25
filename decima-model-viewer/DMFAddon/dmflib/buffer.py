from dataclasses import dataclass
from typing import Dict, Any

from .json_protocol import JsonSerializable


@dataclass
class DMFBuffer(JsonSerializable):
    original_name: str
    buffer_size: int
    buffer_data: str

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["originalName"], data["bufferSize"], data["bufferData"])

    def to_json(self):
        return {
            "originalName": self.original_name,
            "bufferSize": self.buffer_size,
            "bufferData": self.buffer_data,
        }
