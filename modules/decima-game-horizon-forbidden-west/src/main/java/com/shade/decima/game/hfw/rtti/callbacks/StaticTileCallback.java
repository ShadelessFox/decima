package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.Mat34;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.Mat44;
import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

public class StaticTileCallback implements ExtraBinaryDataCallback<StaticTileCallback.StaticTileData> {
    public interface StaticTileData {
        @Attr(name = "Unk01", type = "Array<uint8>", position = 0, offset = 0)
        byte[] unk01();

        void unk01(byte[] value);

        @Attr(name = "Unk02", type = "Array<Mat44>", position = 1, offset = 0)
        List<Mat44> unk02();

        void unk02(List<Mat44> value);

        @Attr(name = "Unk03", type = "Array<uint8>", position = 2, offset = 0)
        byte[] unk03();

        void unk03(byte[] value);

        @Attr(name = "Unk04", type = "Array<uint8>", position = 3, offset = 0)
        byte[] unk04();

        void unk04(byte[] value);

        @Attr(name = "Unk05", type = "Array<Mat34>", position = 4, offset = 0)
        List<Mat34> unk05();

        void unk05(List<Mat34> value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull StaticTileData object) throws IOException {
        object.unk01(reader.readBytes(reader.readInt() * 20));
        object.unk02(reader.readObjects(reader.readInt(), r -> HFWTypeReader.readCompound(Mat44.class, r, factory)));
        object.unk03(reader.readBytes(reader.readInt() * 12));
        object.unk04(reader.readBytes(reader.readInt() * 16));
        object.unk05(reader.readObjects(reader.readInt(), r -> HFWTypeReader.readCompound(Mat34.class, r, factory)));
    }
}
