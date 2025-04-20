package com.shade.decima.game.hrzr.rtti.callbacks;

import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.List;

import static com.shade.decima.game.hrzr.rtti.HorizonZeroDawnRemastered.*;

public class PoseCallback implements ExtraBinaryDataCallback<PoseCallback.PoseData> {
    public interface PoseData {
        @Attr(name = "Unk01", type = "Array<Mat34>", position = 0, offset = 0)
        List<Mat34> unk01();

        void unk01(List<Mat34> value);

        @Attr(name = "Unk02", type = "Array<Mat44>", position = 1, offset = 0)
        List<Mat44> unk02();

        void unk02(List<Mat44> value);

        @Attr(name = "Unk03", type = "Array<float>", position = 2, offset = 0)
        float[] unk03();

        void unk03(float[] value);
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull PoseData object) throws IOException {
        if (!reader.readByteBoolean()) {
            return;
        }

        var count1 = reader.readInt();
        object.unk01(reader.readObjects(count1, r -> readMat34(r, factory)));
        object.unk02(reader.readObjects(count1, r -> readMat44(r, factory)));

        var count2 = reader.readInt();
        object.unk03(reader.readFloats(count2));
    }

    @NotNull
    private static Mat34 readMat34(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        Mat34 mat34 = factory.newInstance(Mat34.class);
        mat34.row0(readVec4Pack(reader, factory));
        mat34.row1(readVec4Pack(reader, factory));
        mat34.row2(readVec4Pack(reader, factory));
        return mat34;
    }

    @NotNull
    private static Mat44 readMat44(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        Mat44 mat44 = factory.newInstance(Mat44.class);
        mat44.col0(readVec4(reader, factory));
        mat44.col1(readVec4(reader, factory));
        mat44.col2(readVec4(reader, factory));
        mat44.col3(readVec4(reader, factory));
        return mat44;
    }

    @NotNull
    private static Vec4 readVec4(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        Vec4 vec4 = factory.newInstance(Vec4.class);
        vec4.x(reader.readFloat());
        vec4.y(reader.readFloat());
        vec4.z(reader.readFloat());
        vec4.w(reader.readFloat());
        return vec4;
    }

    @NotNull
    private static Vec4Pack readVec4Pack(@NotNull BinaryReader reader, @NotNull TypeFactory factory) throws IOException {
        Vec4Pack pack = factory.newInstance(Vec4Pack.class);
        pack.x(reader.readFloat());
        pack.y(reader.readFloat());
        pack.z(reader.readFloat());
        pack.w(reader.readFloat());
        return pack;
    }
}
