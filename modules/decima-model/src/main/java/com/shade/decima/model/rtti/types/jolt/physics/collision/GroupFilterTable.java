package com.shade.decima.model.rtti.types.jolt.physics.collision;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class GroupFilterTable extends GroupFilter {
    private int numSubGroups;
    private byte[] table;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);

        numSubGroups = buffer.getInt();
        table = JoltUtils.getByteArray(buffer);
    }
}
