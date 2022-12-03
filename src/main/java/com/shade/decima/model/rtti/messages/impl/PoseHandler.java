package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(type = "Pose", message = "MsgReadBinary", game = GameType.DS)
public class PoseHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        if (buffer.get() > 0) {
            final int count1 = buffer.getInt();
            object.set("UnknownData1", RTTITypeArray.read(registry, buffer, registry.find("Mat34"), count1));
            object.set("UnknownData2", RTTITypeArray.read(registry, buffer, registry.find("Mat44"), count1));

            final int count2 = buffer.getInt();
            object.set("UnknownData3", RTTITypeArray.read(registry, buffer, registry.find("uint32"), count2));
        }
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
