package com.shade.decima.model.rtti.types.jolt.physics.collision;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public final class CollisionGroup {
    public int groupId;
    public int subGroupId;
    public GroupFilter groupFilter;

    @NotNull
    public static CollisionGroup restoreFromBinaryState(@NotNull ByteBuffer buffer) {
        final CollisionGroup result = new CollisionGroup();
        result.groupId = buffer.getInt();
        result.subGroupId = buffer.getInt();

        return result;
    }
}
