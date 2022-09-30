from dataclasses import dataclass, asdict
from typing import Dict, Any

from .json_serializable_dataclass import JsonSerializable


@dataclass
class DMFCollection(JsonSerializable):
    name: str

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(**data)
