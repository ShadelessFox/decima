package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class PhysicsHeightMapOffsetCollisionResourceCallback implements ExtraBinaryDataCallback<PhysicsHeightMapOffsetCollisionResourceCallback.PhysicsHeightMapOffsetCollisionData> {
    public interface PhysicsHeightMapOffsetCollisionData {
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull PhysicsHeightMapOffsetCollisionData object) throws IOException {

    }
}
