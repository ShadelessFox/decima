package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class NavMeshCallback implements ExtraBinaryDataCallback<NavMeshCallback.NavMeshData> {
    private static final int NAVMESHSET_MAGIC = 'M' << 24 | 'S' << 16 | 'E' << 8 | 'T';
    private static final int NAVMESHSET_VERSION = 1;

    public interface NavMeshData {
        @RTTI.Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull ByteBuffer buffer, @NotNull NavMeshData object) {
        var start = buffer.position();
        var navMeshSetMagic = buffer.getInt();
        var navMeshSetVersion = buffer.getInt();
        var numTiles = buffer.getInt();
        if (navMeshSetMagic != NAVMESHSET_MAGIC || navMeshSetVersion != NAVMESHSET_VERSION) {
            throw new IllegalArgumentException("Invalid nav mesh set header");
        }

        buffer.position(buffer.position() + 28); // dtNavMeshParams
        for (int i = 0; i < numTiles; i++) {
            int tileStart = buffer.position() + 8;
            var polysSize = buffer.getInt(tileStart + 24) * 33;
            var vertsSize = buffer.getInt(tileStart + 28) * 12;
            var linksSize = buffer.getInt(tileStart + 32) * 12;
            var detailMeshesSize = buffer.getInt(tileStart + 36) * 10;
            var detailVertsSize = buffer.getInt(tileStart + 40) * 12;
            var detailTrisSize = buffer.getInt(tileStart + 44) * 4;
            var bVNodesSize = buffer.getInt(tileStart + 48) * 16;
            var offMeshConsSize = buffer.getInt(tileStart + 52) * 32;
            buffer.position(tileStart + 100 + polysSize + vertsSize + linksSize + detailMeshesSize + detailVertsSize + detailTrisSize + bVNodesSize + offMeshConsSize);
        }

        var size = buffer.position() - start;
        var data = BufferUtils.getBytes(buffer.position(start), size);

        object.data(data);
    }
}
