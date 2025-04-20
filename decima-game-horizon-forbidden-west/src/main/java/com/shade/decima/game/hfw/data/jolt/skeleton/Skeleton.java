package com.shade.decima.game.hfw.data.jolt.skeleton;

import com.shade.decima.game.hfw.data.jolt.JoltUtils;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public record Skeleton(@NotNull List<Joint> joints) {
    public record Joint(@NotNull String name, @NotNull String parentName, int parentJointIndex) {
        @NotNull
        private static Joint read(@NotNull BinaryReader reader) throws IOException {
            var name = JoltUtils.readString(reader);
            var parentJointIndex = reader.readInt();
            var parentName = JoltUtils.readString(reader);

            return new Joint(name, parentName, parentJointIndex);
        }
    }

    @NotNull
    public static Skeleton restoreFromBinaryState(@NotNull BinaryReader reader) throws IOException {
        var joints = reader.readObjects(reader.readInt(), Joint::read);

        return new Skeleton(joints);
    }
}
