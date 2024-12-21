package com.shade.decima.game.hfw.data.jolt.physics.collision;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class GroupFilterTable extends GroupFilter {
    public int numSubGroups;
    public byte[] table;

    @Override
    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        super.restoreBinaryState(reader);

        numSubGroups = reader.readInt();
        table = JoltUtils.readBytes(reader);
    }
}
