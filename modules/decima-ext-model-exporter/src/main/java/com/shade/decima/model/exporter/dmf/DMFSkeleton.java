package com.shade.decima.model.exporter.dmf;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DMFSkeleton {
    public final List<DMFBone> bones = new ArrayList<>();

    @NotNull
    public DMFBone newBone(@NotNull String name, @NotNull DMFTransform transform, int parentId) {
        final DMFBone bone = new DMFBone(name, transform, parentId);
        bones.add(bone);
        return bone;
    }

    @Nullable
    public DMFBone findBone(@NotNull String name) {
        for (DMFBone bone : bones) {
            if (bone.name.equals(name)) {
                return bone;
            }
        }
        return null;
    }

    public int findBoneId(@NotNull String name) {
        for (int i = 0; i < bones.size(); i++) {
            if (bones.get(i).name.equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
