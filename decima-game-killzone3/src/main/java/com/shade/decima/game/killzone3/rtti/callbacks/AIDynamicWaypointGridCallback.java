package com.shade.decima.game.killzone3.rtti.callbacks;

import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class AIDynamicWaypointGridCallback implements ExtraBinaryDataCallback<AIDynamicWaypointGridCallback.AIDynamicWaypointGridData> {
    public interface AIDynamicWaypointGridData {
    }

    @Override
    public void deserialize(BinaryReader reader, TypeFactory factory, AIDynamicWaypointGridData object) throws IOException {
        reader.align(4);

        var count = reader.readInt();
        for (int i = 0; i < count; i++) {
            var unk00 = reader.readInt();
            var unk04 = reader.readFloat();
            var unk08 = reader.readShort();
            var unk0A = reader.readShort();
        }

        var count2 = reader.readInt();
        for (int i = 0; i < count2; i++) {
            var minX = reader.readFloat();
            var minY = reader.readFloat();
            var minZ = reader.readFloat();
            var maxX = reader.readFloat();
            var maxY = reader.readFloat();
            var maxZ = reader.readFloat();
            var unk = reader.readShort();
        }
    }
}
