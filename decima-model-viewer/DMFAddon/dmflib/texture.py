from dataclasses import dataclass
from enum import Enum
from typing import Dict, Any

from .json_protocol import JsonSerializable


class DMFDataType(Enum):
    DDS = "DDS"
    PNG = "PNG"


@dataclass
class DMFTexture(JsonSerializable):
    name: str
    data_type: DMFDataType
    buffer_size: int
    usage_type: int
    metadata: Dict[str, str]

    def to_json(self):
        raise NotImplementedError()

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        if "bufferData" in data:
            return DMFInternalTexture.from_json(data)
        else:
            return DMFExternalTexture.from_json(data)


@dataclass
class DMFInternalTexture(DMFTexture):
    buffer_data: str

    def to_json(self):
        return {
            "name": self.name,
            "bufferData": self.buffer_data,
            "bufferSize": self.buffer_size,
            "dataType": self.data_type,
            "usageType": self.usage_type,
            "metadata": self.metadata

        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], DMFDataType(data["dataType"]), data["bufferSize"],
                   data.get("usageType", 0),data.get("metadata", {}), data["bufferData"])


@dataclass
class DMFExternalTexture(DMFTexture):
    buffer_file_name: str

    def to_json(self):
        return {
            "name": self.name,
            "bufferFileName": self.buffer_file_name,
            "bufferSize": self.buffer_size,
            "dataType": self.data_type,
            "usageType": self.usage_type,
            "metadata": self.metadata

        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], DMFDataType(data["dataType"]), data["bufferSize"],
                   data.get("usageType", 0),data.get("metadata", {}), data["bufferFileName"])
