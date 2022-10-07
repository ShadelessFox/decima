from dataclasses import dataclass, asdict
from enum import Enum
from typing import List, Dict, Any, Optional

from .json_serializable_dataclass import JsonSerializable
from .transform import DMFTransform

from .mesh import DMFMesh


class DMFNodeType(Enum):
    Node = "Node"
    Model = "Model"
    ModelGroup = "ModelGroup"


@dataclass
class DMFNode(JsonSerializable):
    type: DMFNodeType
    name: Optional[str]
    collection_ids: List[int]
    transform: Optional[DMFTransform]
    children: List['DMFNode']

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        if data is None:
            return None
        node_type = DMFNodeType(data["type"])
        name = data.get("name", node_type.name)
        collection_ids = data.get('collectionIds', [])
        transform = DMFTransform.from_json(data["transform"]) if "transform" in data else None
        children = [cls.from_json(item) for item in data.get("children", [])]
        if node_type == DMFNodeType.Model:
            return DMFModel(node_type, name, collection_ids, transform, children,
                            DMFMesh.from_json(data["mesh"]), data.get("skeletonId", None))
        elif node_type == DMFNodeType.ModelGroup:
            return DMFModelGroup(node_type, name, collection_ids, transform, children)
        else:
            return DMFNode(node_type, name, collection_ids, transform, children)


@dataclass
class DMFModelGroup(DMFNode):
    pass


@dataclass
class DMFModel(DMFNode):
    mesh: DMFMesh
    skeleton_id: int
