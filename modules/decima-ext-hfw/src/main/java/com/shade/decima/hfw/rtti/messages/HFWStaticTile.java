package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.model.base.GameType;
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
    @Type(name = "StaticTile", game = GameType.HFW)
})
public class HFWStaticTile implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        object.set("UnknownData1", RTTITypeArray.read(factory, buffer, factory.find("uint8"), buffer.getInt() * 20));
        object.set("UnknownData2", RTTITypeArray.read(factory, buffer, factory.find("Mat44"), buffer.getInt()));
        object.set("UnknownData3", RTTITypeArray.read(factory, buffer, factory.find("uint8"), buffer.getInt() * 12));
        object.set("UnknownData4", RTTITypeArray.read(factory, buffer, factory.find("uint8"), buffer.getInt() * 16));
        object.set("UnknownData5", RTTITypeArray.read(factory, buffer, factory.find("Mat34"), buffer.getInt()));
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTIFactory factory, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("UnknownData1", factory.find("Array<uint8>")),
            new Component("UnknownData2", factory.find("Array<Mat44>")),
            new Component("UnknownData3", factory.find("Array<uint8>")),
            new Component("UnknownData4", factory.find("Array<uint8>")),
            new Component("UnknownData5", factory.find("Array<Mat34>")),
        };
    }
}
