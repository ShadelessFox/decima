package com.shade.decima.game.killzone3.rtti.callbacks;

import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class StaticMeshInstanceCallback implements ExtraBinaryDataCallback<StaticMeshInstanceCallback.StaticMeshInstanceData> {
    public interface StaticMeshInstanceData {
    }

    @Override
    public void deserialize(BinaryReader reader, TypeFactory factory, StaticMeshInstanceData object) throws IOException {
        // Nothing to read
    }
}
