from dataclasses import dataclass, asdict
from typing import Dict, Any, Optional

from .json_serializable_dataclass import JsonSerializable


@dataclass
class DMFCollection(JsonSerializable):
    name: str
    enabled: bool
    parent: Optional[int]

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], data.get("enabled", True), data.get("parent", None))
