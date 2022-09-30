package com.shade.decima.ui.data.viewer.mesh.dmf;


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
}
