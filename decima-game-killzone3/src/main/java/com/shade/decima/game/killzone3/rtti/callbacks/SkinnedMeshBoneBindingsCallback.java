package com.shade.decima.game.killzone3.rtti.callbacks;

import com.shade.decima.game.killzone3.rtti.Killzone3.ProjMatrix;
import com.shade.decima.game.killzone3.rtti.Killzone3.SkinnedMeshBoneBindings;
import com.shade.decima.game.killzone3.rtti.Killzone3TypeReader;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class SkinnedMeshBoneBindingsCallback implements ExtraBinaryDataCallback<SkinnedMeshBoneBindingsCallback.SkinnedMeshBoneBindingsData> {
    public interface SkinnedMeshBoneBindingsData {
    }

    @Override
    public void deserialize(BinaryReader reader, TypeFactory factory, SkinnedMeshBoneBindingsData object) throws IOException {
        var self = (SkinnedMeshBoneBindings) object;
        var inverseBindMatrices = reader.readObjects(self.boneNames().size(), r -> Killzone3TypeReader.readCompound(ProjMatrix.class, r, factory));
        self.inverseBindMatrices(inverseBindMatrices);
    }
}
