package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class MorphemeNetworkDefResourceCallback implements ExtraBinaryDataCallback<MorphemeNetworkDefResourceCallback.MorphemeNetworkDefData> {
    public interface MorphemeNetworkDefData {
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull MorphemeNetworkDefData object) throws IOException {

    }
}
