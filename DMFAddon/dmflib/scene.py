from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Dict, Any, Optional

from .buffer import DMFBuffer
from .buffer_view import DMFBufferView
from .collection import DMFCollection
from .json_serializable_dataclass import JsonSerializable
from .node import DMFNode
from .scene_meta_data import DMFSceneMetaData
from .skeleton import DMFSkeleton


@dataclass
class DMFSceneFile(JsonSerializable):
    meta_data: DMFSceneMetaData
    collections: List[DMFCollection]
    models: List[DMFNode]
    skeletons: List[DMFSkeleton]
    buffers: List[DMFBuffer]
    buffer_views: List[DMFBufferView]

    _buffers_path: Optional[Path] = field(default=None)

    def to_json(self):
        return {
            "metadata": self.meta_data.to_json(),
            "collection": [item.to_json() for item in self.collections],
            "models": [item.to_json() for item in self.models],
            "buffers": [item.to_json() for item in self.buffers],
            "bufferViews": [item.to_json() for item in self.buffer_views],
        }

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(
            DMFSceneMetaData.from_json(data["metadata"]),
            [DMFCollection.from_json(item) for item in data.get("collection", [])],
            [DMFNode.from_json(item) for item in data.get("models", [])],
            [DMFSkeleton.from_json(item) for item in data.get("skeletons", [])],
            [DMFBuffer.from_json(item) for item in data.get("buffers", [])],
            [DMFBufferView.from_json(item) for item in data.get("bufferViews", [])],
        )

    # materials: List[DMFMaterial]
    # textures: List[DMFTexture]

    def set_buffers_path(self, buffers_path: Path):
        self._buffers_path = buffers_path

    @property
    def buffers_path(self):
        assert self._buffers_path
        return self._buffers_path
