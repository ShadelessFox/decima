package com.shade.decima.game.hfw.data.jolt.physics.collision;

import com.shade.decima.game.hfw.data.jolt.core.Factory;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class GroupFilter {
    @NotNull
    public static GroupFilter sRestoreFromBinaryState(@NotNull BinaryReader reader) throws IOException {
        var hash = reader.readInt();
        var name = Factory.getTypeName(hash);
        var result = switch (name) {
            case "GroupFilterTable" -> new GroupFilterTable();
            default -> throw new NotImplementedException();
        };
        result.restoreBinaryState(reader);
        return result;
    }

    public void restoreBinaryState(@NotNull BinaryReader reader) throws IOException {
        // no-op
    }
}
