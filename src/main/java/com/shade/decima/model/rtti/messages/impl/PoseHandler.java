package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.data.registry.Type;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "Pose", game = GameType.DS),
    @Type(name = "Pose", game = GameType.DSDC),
    @Type(name = "Pose", game = GameType.HZD)
})
public class PoseHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        if (buffer.get() > 0) {
            final int count1 = buffer.getInt();
            object.set("UnknownData1", RTTITypeArray.read(registry, buffer, registry.find("Mat34"), count1));
            object.set("UnknownData2", RTTITypeArray.read(registry, buffer, registry.find("Mat44"), count1));

            final int count2 = buffer.getInt();
            object.set("UnknownData3", RTTITypeArray.read(registry, buffer, registry.find("uint32"), count2));
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTIObject[] data1 = object.objs("UnknownData1");
        final RTTIObject[] data2 = object.objs("UnknownData2");
        final RTTIObject[] data3 = object.objs("UnknownData3");

        buffer.putInt(data1.length);

        for (RTTIObject obj : data1) {
            object.type().write(registry, buffer, obj);
        }

        for (RTTIObject obj : data2) {
            object.type().write(registry, buffer, obj);
        }

        buffer.putInt(data3.length);

        for (RTTIObject obj : data3) {
            object.type().write(registry, buffer, obj);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        final RTTIObject[] data1 = object.objs("UnknownData1");
        final RTTIObject[] data3 = object.objs("UnknownData3");
        return data1.length * 112 + data3.length * 4;
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("UnknownData1", registry.find("Array<Mat34>")),
            new Component("UnknownData2", registry.find("Array<Mat44>")),
            new Component("UnknownData3", registry.find("Array<uint32>")),
        };
    }
}
