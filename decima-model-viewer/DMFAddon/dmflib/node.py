from dataclasses import dataclass, asdict
from enum import Enum
from typing import List, Dict, Any, Optional

from .json_protocol import JsonSerializable
from .transform import DMFTransform

from .mesh import DMFMesh


class DMFNodeType(Enum):
    Node = "Node"
    Model = "Model"
    ModelGroup = "ModelGroup"
    LOD = "LOD"


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
            return DMFModel(node_type, name, collection_ids, transform, children, data.get("visible", True),
                            DMFMesh.from_json(data["mesh"]), data.get("skeletonId", None))
        elif node_type == DMFNodeType.ModelGroup:
            return DMFModelGroup(node_type, name, collection_ids, transform, children, data.get("visible", True))
        elif node_type == DMFNodeType.LOD:
            return DMFLodModel(node_type, name, collection_ids, transform, children, data.get("visible", True),
                               [DMFLodModel.Lod.from_json(lod_data) for lod_data in data.get("lods", [])])
        else:
            return DMFNode(node_type, name, collection_ids, transform, children, data.get("visible", True))


@dataclass
class DMFModelGroup(DMFNode):
    pass


@dataclass
class DMFModel(DMFNode):
    mesh: DMFMesh
    skeleton_id: int


@dataclass
class DMFLodModel(DMFNode):
    @dataclass
    class Lod(JsonSerializable):
        model: DMFNode
        lod_id: int
        distance: float

        @classmethod
        def from_json(cls, data: Dict[str, Any]):
            return cls(DMFNode.from_json(data.get("model", None)), data["id"], data["distance"])

    lods: List[Lod]
