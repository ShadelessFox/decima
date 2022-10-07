import base64
from dataclasses import dataclass
from enum import Enum
from typing import Dict, Any

from .json_serializable_dataclass import JsonSerializable


class DMFDataType(Enum):
    DDS = "DDS"


@dataclass
class DMFTexture(JsonSerializable):
    name: str
    embedded_data: str
    embedded_data_size: int
    data_type: DMFDataType

    def to_json(self):
        return {
            "name": self.name,
            "embeddedData": self.embedded_data,
            "embeddedDataSize": self.embedded_data_size,
            "dataType": self.data_type

        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], data["embeddedData"], data["embeddedDataSize"], DMFDataType(data["dataType"]))

    def get_data(self):
        data = base64.b64decode(self.embedded_data)
        assert len(data) == self.embedded_data_size
        return data
