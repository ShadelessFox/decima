from dataclasses import dataclass, asdict
from typing import Tuple, Dict, Any

from .json_protocol import JsonSerializable


@dataclass
class DMFTransform(JsonSerializable):
    position: Tuple[float, float, float]
    scale: Tuple[float, float, float]
    rotation: Tuple[float, float, float, float]

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(**data)
