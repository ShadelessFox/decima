package com.shade.decima.ui.data.viewer.model.abstractModel;

import com.shade.decima.ui.data.viewer.model.utils.Matrix4x4;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Skeleton {

    private final List<Bone> bones = new ArrayList<>();

    public Skeleton() {
    }

    @NotNull
    public List<Bone> getBones() {
        return bones;
    }

    @NotNull
    public Bone addBone(@NotNull String name, @NotNull Matrix4x4 transform, int parent) {
        Bone bone = findByName(name);
        if (bone == null) {
            bone = new Bone(name, transform, parent);
            bones.add(bone);
        } else if (!bone.transform.closeEnough(transform)) {
            throw new IllegalStateException("Matrices on same bones does not match");
        }

        return bone;
    }

    @NotNull
    public Bone addBone(@NotNull String name, @NotNull Matrix4x4 transform) {
        Bone bone = new Bone(name, transform, -1);
        bones.add(bone);
        return bone;
    }

    @NotNull
    public Bone addBone(@NotNull String name) {
        Bone bone = new Bone(name);
        bone.parent = -1;
        bones.add(bone);
        return bone;
    }

    @Nullable
    public Bone findByName(@NotNull String name) {
        for (Bone bone : bones) {
            if (bone.name.equals(name))
                return bone;
        }
        return null;
    }

    public Bone get(int id) {
        return bones.get(id);
    }

    public int getBoneId(@NotNull Bone bone) {
        return bones.indexOf(bone);
    }

    public int getBoneId(@NotNull String name) {
        for (int boneId = 0; boneId < bones.size(); boneId++) {
            if (bones.get(boneId).name.equals(name))
                return boneId;
        }
        return -1;
    }

    public Matrix4x4 toRelative(@NotNull Bone bone) {
        if (bone.parent == -1 || bone.isRelative)
            return bone.transform;
        Bone parent = bones.get(bone.parent);
        return parent.transform.inverted().matMul(bone.transform);
    }

    public Matrix4x4 toAbsolute(@NotNull Bone bone) {
        if (bone.parent == -1 || !bone.isRelative)
            return bone.transform;
        Bone parent = bones.get(bone.parent);
        return toAbsolute(parent).matMul(bone.transform);
    }

    public Matrix4x4 getInverseBindMatrix(@NotNull Bone bone) {
        if (bone.inverseBindMatrix != null)
            return bone.inverseBindMatrix;
        return toAbsolute(bone).inverted();
    }


}
