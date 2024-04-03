package com.shade.decima.hfw.rtti.messages;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "Pose", game = GameType.HFW)
})
public class HFWPoseHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        if (buffer.get() != 0) {
            final int count1 = buffer.getInt();
            object.set("UnknownData1", RTTITypeArray.read(registry, buffer, registry.find("Mat34"), count1));
            object.set("UnknownData2", RTTITypeArray.read(registry, buffer, registry.find("Mat34"), count1));

            final int count2 = buffer.getInt();
            object.set("UnknownData3", RTTITypeArray.read(registry, buffer, registry.find("uint32"), count2));
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        throw new NotImplementedException();
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
