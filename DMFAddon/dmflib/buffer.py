import base64
from pathlib import Path
from typing import Dict, Any, Optional

from .json_serializable_dataclass import JsonSerializable


class DMFBuffer(JsonSerializable):
    def __init__(self):
        self._buffer_data = b''
        self._buffer_size = 0
        self._buffer_path: Optional[str] = None

    def to_json(self):
        assert self._buffer_data, "Buffer is not properly initialized"
        assert self._buffer_size, "Buffer is not properly initialized"
        return {"data": base64.b64encode(self._buffer_data).decode("ascii"), "data_size": len(self._buffer_data)}

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        self = cls()
        self._buffer_size = data["bufferSize"]
        if "bufferData" in data:
            self._buffer_data = base64.b64decode(data["bufferData"])
            assert len(self._buffer_data) == self._buffer_size
        elif "bufferFileName" in data:
            self._buffer_path = data["bufferFileName"]
        return self

    def get_data(self, buffers_path: Path):
        if self._buffer_data:
            return self._buffer_data
        elif self._buffer_path:
            buffer_path = buffers_path / self._buffer_path
            assert buffer_path.exists()
            self._buffer_data = data = buffer_path.open('rb').read()
            assert len(data) == self._buffer_size
            return data
        raise ValueError("Failed to get buffer data")
