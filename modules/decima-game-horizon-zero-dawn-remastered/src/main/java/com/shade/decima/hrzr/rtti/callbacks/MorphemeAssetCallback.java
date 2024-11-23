package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.TypeFactory;
import com.shade.decima.rtti.data.meta.Attr;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class MorphemeAssetCallback implements ExtraBinaryDataCallback<MorphemeAssetCallback.MorphemeAssetData> {
    public interface MorphemeAssetData {
        @Attr(name = "Data", type = "Array<uint8>", position = 0, offset = 0)
        byte[] data();

        void data(byte[] value);

        @Attr(name = "AssetID", type = "uint32", position = 1, offset = 0)
        int assetID();

        void assetID(int value);

        @Attr(name = "AssetType", type = "int32", position = 2, offset = 0)
        int assetType();

        void assetType(int value);

        @Attr(name = "AssetSize", type = "int64", position = 3, offset = 0)
        long assetSize();

        void assetSize(long value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull MorphemeAssetData object) throws IOException {
        object.data(reader.readBytes(reader.readInt()));
        object.assetID(reader.readInt());
        object.assetType(reader.readInt());
        object.assetSize(reader.readLong());
    }
}
