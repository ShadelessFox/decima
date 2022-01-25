package com.shade.decima.rtti.messages.impl;

import com.shade.decima.base.GameType;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.messages.RTTIMessageHandler;
import com.shade.decima.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.RTTIUtils;

import java.nio.ByteBuffer;

@RTTIMessageHandler(type = "Pose", message = "MsgReadBinary", game = GameType.DS)
public class PoseMessageHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        if (buffer.get() > 0) {
            final RTTIType<?> mat34 = registry.find("Mat34");
            final RTTIType<?> mat44 = registry.find("Mat44");
            final RTTIType<?> uint32 = registry.find("uint32");

            final int count1 = buffer.getInt();
            object.set("UnknownData1", RTTIUtils.readCollection(registry, buffer, mat34, count1), true);
            object.set("UnknownData2", RTTIUtils.readCollection(registry, buffer, mat44, count1), true);

            final int count2 = buffer.getInt();
            object.set("UnknownData3", RTTIUtils.readCollection(registry, buffer, uint32, count2), true);
        }
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }
}
