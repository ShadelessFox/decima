package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.game.hfw.rtti.HFWTypeReader;
import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

import static com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.Mat34;

public class PoseCallback implements ExtraBinaryDataCallback<PoseCallback.PoseData> {
    public interface PoseData {
        @Attr(name = "Unk01", type = "Array<Mat34>", position = 0, offset = 0)
        List<Mat34> unk01();

        void unk01(List<Mat34> value);

        @Attr(name = "Unk02", type = "Array<Mat34>", position = 1, offset = 0)
        List<Mat34> unk02();

        void unk02(List<Mat34> value);

        @Attr(name = "Unk03", type = "Array<uint32>", position = 2, offset = 0)
        int[] unk03();

        void unk03(int[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull PoseData object) throws IOException {
        if (reader.readByteBoolean()) {
            var count1 = reader.readInt();
            object.unk01(reader.readObjects(count1, r -> HFWTypeReader.readCompound(Mat34.class, reader, factory)));
            object.unk02(reader.readObjects(count1, r -> HFWTypeReader.readCompound(Mat34.class, reader, factory)));

            var count2 = reader.readInt();
            object.unk03(reader.readInts(count2));
        }
    }
}
