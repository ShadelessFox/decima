package com.shade.decima.ui.data.viewer.model.model.dmf;


import java.util.ArrayList;
import java.util.List;

public class DMFSkeleton {
    public List<DMFBone> bones = new ArrayList<>();
    public DMFTransform transform;

    public DMFBone newBone(String name, DMFTransform transform, int parentId) {
        DMFBone bone = new DMFBone(name, transform, parentId);
        bones.add(bone);
        return bone;
    }

    public DMFBone newBone(String name, DMFTransform transform) {
        DMFBone bone = new DMFBone(name, transform, -1);
        bones.add(bone);
        return bone;
    }

    public DMFBone findBone(String name) {
        for (DMFBone bone : bones) {
            if (bone.name.equals(name)) {
                return bone;
            }
        }
        return null;
    }

    public int findBoneId(String name) {
        for (int i = 0; i < bones.size(); i++) {
            DMFBone bone = bones.get(i);
            if (bone.name.equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
