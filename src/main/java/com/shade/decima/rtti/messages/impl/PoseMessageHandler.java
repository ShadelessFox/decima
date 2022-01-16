package com.shade.decima.rtti.messages.impl;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.messages.RTTIMessageHandler;
import com.shade.decima.rtti.messages.RTTIMessageReadBinary;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

@RTTIMessageHandler(type = "Pose", message = "MsgReadBinary")
public class PoseMessageHandler implements RTTIMessageReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        if (buffer.get() > 0) {
            final RTTIType<?> mat34 = RTTITypeRegistry.getInstance().find("Mat34");
            final RTTIType<?> mat44 = RTTITypeRegistry.getInstance().find("Mat44");
            final RTTIType<?> uint32 = RTTITypeRegistry.getInstance().find("uint32");

            final int count1 = buffer.getInt();
            defineVirtualMember(object, "UnknownData1", "Array<Mat34>", IntStream.range(0, count1).mapToObj(x -> mat34.read(buffer)).toArray());
            defineVirtualMember(object, "UnknownData2", "Array<Mat44>", IntStream.range(0, count1).mapToObj(x -> mat44.read(buffer)).toArray());

            final int count2 = buffer.getInt();
            defineVirtualMember(object, "UnknownData3", "Array<uint32>", IntStream.range(0, count2).mapToObj(x -> uint32.read(buffer)).toArray());
        }
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull ByteBuffer buffer) {
        throw new IllegalStateException("Not implemented");
    }

    private static void defineVirtualMember(@NotNull RTTIObject object, @NotNull String name, @NotNull String type, @NotNull Object value) {
        object.getMembers().put(new RTTITypeClass.Member(object.getType(), RTTITypeRegistry.getInstance().find(type), name, "", 0, 0), value);
    }
}
