from dataclasses import dataclass, asdict, field
from typing import Dict, Any, Optional

from .json_protocol import JsonSerializable


@dataclass
class DMFCollection(JsonSerializable):
    name: str
    enabled: bool = field(default=True)
    parent: Optional[int] = field(default=None)

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(data["name"], data.get("enabled", True), data.get("parent", None))
