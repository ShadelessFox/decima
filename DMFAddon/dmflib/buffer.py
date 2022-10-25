import base64
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Any, Optional

from .json_serializable_dataclass import JsonSerializable


@dataclass
class DMFBuffer(JsonSerializable):
    original_name: str
    buffer_size: int

    def to_json(self):
        raise NotImplementedError()

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        if "bufferData" in data:
            return DMFInternalBuffer.from_json(data)
        else:
            return DMFExternalBuffer.from_json(data)


@dataclass
class DMFExternalBuffer(DMFBuffer):
    buffer_file_name: str

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["originalName"], data["bufferSize"], data["bufferFileName"])

    def to_json(self):
        return {
            "originalName": self.original_name,
            "bufferSize": self.buffer_size,
            "bufferFileName": self.buffer_file_name,
        }


@dataclass
class DMFInternalBuffer(DMFBuffer):
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
