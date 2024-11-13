package com.shade.decima.rtti.callbacks;

import com.shade.decima.rtti.RTTI;
import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class NavMeshCallback implements ExtraBinaryDataCallback<NavMeshCallback.NavMeshData> {
    private static final int NAVMESHSET_MAGIC = 'M' << 24 | 'S' << 16 | 'E' << 8 | 'T';
    private static final int NAVMESHSET_VERSION = 1;

    public interface NavMeshData {
        @RTTI.Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull NavMeshData object) throws IOException {
        var start = reader.position();
        var navMeshSetMagic = reader.readInt();
        var navMeshSetVersion = reader.readInt();
        var numTiles = reader.readInt();
        if (navMeshSetMagic != NAVMESHSET_MAGIC || navMeshSetVersion != NAVMESHSET_VERSION) {
            throw new IllegalArgumentException("Invalid nav mesh set header");
        }

        reader.position(reader.position() + 28); // dtNavMeshParams
        for (int i = 0; i < numTiles; i++) {
            var tileStart = reader.position() + 8;
            reader.position(tileStart + 24);
            var polysSize = reader.readInt() * 33;
            reader.position(tileStart + 28);
            var vertsSize = reader.readInt() * 12;
            reader.position(tileStart + 32);
            var linksSize = reader.readInt() * 12;
            reader.position(tileStart + 36);
            var detailMeshesSize = reader.readInt() * 10;
            reader.position(tileStart + 40);
            var detailVertsSize = reader.readInt() * 12;
            reader.position(tileStart + 44);
            var detailTrisSize = reader.readInt() * 4;
            reader.position(tileStart + 48);
            var bVNodesSize = reader.readInt() * 16;
            reader.position(tileStart + 52);
            var offMeshConsSize = reader.readInt() * 32;
            reader.position(tileStart + 100 + polysSize + vertsSize + linksSize + detailMeshesSize + detailVertsSize + detailTrisSize + bVNodesSize + offMeshConsSize);
        }

        var size = reader.position() - start;
        reader.position(start);
        var data = reader.readBytes(Math.toIntExact(size));

        object.data(data);
    }
}
