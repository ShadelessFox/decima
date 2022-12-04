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
    visible: bool

    def to_json(self):
        return asdict(self)

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        if data is None:
            return None
        node_type = DMFNodeType(data["type"])
        name = data.get("name", node_type.name)
        collection_ids = list(set(data.get('collectionIds', [])))
        transform = DMFTransform.from_json(data["transform"]) if "transform" in data else None
        children = [cls.from_json(item) for item in data.get("children", [])]
        if node_type == DMFNodeType.Model:
            # remap_table = {v: n for n, v in enumerate(data.get("boneRemapTable", []))}
            remap_table = {int(k): v for k, v in data.get("boneRemapTable", {}).items()}

            return DMFModel(node_type, name, collection_ids, transform, children, data.get("visible", True),
                            DMFMesh.from_json(data["mesh"]), remap_table,
                            data.get("skeletonId", None))
        elif node_type == DMFNodeType.ModelGroup:
            return DMFModelGroup(node_type, name, collection_ids, transform, children, data.get("visible", True))
        else:
            return DMFNode(node_type, name, collection_ids, transform, children, data.get("visible", True))


@dataclass
class DMFModelGroup(DMFNode):
    pass


@dataclass
class DMFModel(DMFNode):
    mesh: DMFMesh
    bone_remap_table: Dict[int, int]
    # bone_remap_table: List[int]
    skeleton_id: int
