package com.shade.decima.model.rtti.messages.shared;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "Pose", game = GameType.DS),
    @Type(name = "Pose", game = GameType.DSDC),
    @Type(name = "Pose", game = GameType.HZD)
})
public class PoseHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        if (buffer.get() != 0) {
            final int count1 = buffer.getInt();
            object.set("UnknownData1", RTTITypeArray.read(factory, reader, buffer, factory.find("Mat34"), count1));
            object.set("UnknownData2", RTTITypeArray.read(factory, reader, buffer, factory.find("Mat44"), count1));

            final int count2 = buffer.getInt();
            object.set("UnknownData3", RTTITypeArray.read(factory, reader, buffer, factory.find("uint32"), count2));
        }
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final boolean present = object.get("UnknownData1") != null;
        buffer.put((byte) (present ? 1 : 0));

        if (present) {
            final var data1 = object.objs("UnknownData1");
            final var data2 = object.objs("UnknownData2");
            final var data3 = object.ints("UnknownData3");

            buffer.putInt(data1.length);
            for (RTTIObject obj : data1) {
                obj.type().write(factory, buffer, obj);
            }
            for (RTTIObject obj : data2) {
                obj.type().write(factory, buffer, obj);
            }

            buffer.putInt(data3.length);
            for (int value : data3) {
                buffer.putInt(value);
            }
        }
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        final var data1 = object.objs("UnknownData1");
        final var data2 = object.objs("UnknownData2");
        final var data3 = object.ints("UnknownData3");
        return data1.length * 48 + data2.length * 64 + data3.length * 4 + 9;
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("UnknownData1", factory.find("Array<Mat34>")),
            new Component("UnknownData2", factory.find("Array<Mat44>")),
            new Component("UnknownData3", factory.find("Array<uint32>")),
        };
    }
}
