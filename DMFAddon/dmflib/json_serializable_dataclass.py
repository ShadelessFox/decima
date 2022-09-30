from dataclasses import dataclass
from enum import Enum
from typing import Dict, Any


@dataclass
class JsonSerializable:

    def to_json(self):
        raise NotImplementedError()

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        raise NotImplementedError()


def json_default(obj):
    if isinstance(obj, JsonSerializable):
        return obj.to_json()
    elif isinstance(obj, Enum):
        return obj.value
    raise TypeError(f'Object of type {obj.__class__.__name__} is not JSON serializable')
