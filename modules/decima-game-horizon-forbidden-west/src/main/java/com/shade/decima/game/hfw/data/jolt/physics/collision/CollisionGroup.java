package com.shade.decima.game.hfw.data.jolt.physics.collision;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public final class CollisionGroup {
    public int groupId;
    public int subGroupId;
    public GroupFilter groupFilter;

    @NotNull
    public static CollisionGroup restoreFromBinaryState(@NotNull BinaryReader reader) throws IOException {
        CollisionGroup result = new CollisionGroup();
        result.groupId = reader.readInt();
        result.subGroupId = reader.readInt();

        return result;
    }
}
