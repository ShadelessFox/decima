from dataclasses import dataclass, asdict
from typing import List, Dict, Any, Optional

from .bone import DMFBone
from .json_serializable_dataclass import JsonSerializable

from .transform import DMFTransform


@dataclass
class DMFSkeleton(JsonSerializable):
    bones: List[DMFBone]
    transform: Optional[DMFTransform]

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        transform = DMFTransform.from_json(data["transform"]) if "transform" in data else None
        return cls([DMFBone.from_json(item) for item in data.get("bones", [])], transform)
