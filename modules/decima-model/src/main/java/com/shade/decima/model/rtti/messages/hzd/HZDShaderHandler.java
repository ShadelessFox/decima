package com.shade.decima.model.rtti.messages.hzd;

import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

// @MessageHandlerRegistration(message = "MsgReadBinary", types = {
//     @Type(name = "ShaderResource", game = GameType.HZD)
// })
public class HZDShaderHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final var size = buffer.getInt();
        final var hash = factory.find("MurmurHashValue").read(factory, reader, buffer);
        final var unk1 = buffer.getInt();
        final var programTypeMask = factory.<RTTITypeEnum>find("EProgramTypeMask").valueOf(buffer.getInt());
        final var unk2 = buffer.getInt();
        final var programs = BufferUtils.getObjects(buffer, 4, RTTIObject[]::new, buf -> ProgramEntry.read(factory, buf));
        final var rootSignature = BufferUtils.getBytes(buffer, buffer.getInt());

        object.set("Hash", hash);
        object.set("Unk1", unk1);
        object.set("ProgramTypeMask", programTypeMask);
        object.set("Unk2", unk2);
        object.set("Programs", programs);
        object.set("RootSignature", rootSignature);
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("Hash", factory.find("MurmurHashValue")),
            new Component("Unk1", factory.find("uint32")),
            new Component("ProgramTypeMask", factory.find("EProgramTypeMask")),
            new Component("Unk2", factory.find("uint32")),
            new Component("Programs", factory.find(ProgramEntry[].class)),
            new Component("RootSignature", factory.find("Array<uint8>"))
        };
    }

    public static class ProgramEntry {
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;
        @RTTIField(type = @Type(name = "EProgramType"))
        public RTTITypeEnum.Constant programType;

        public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
            final var unk0 = buffer.getInt();
            final var unk1 = buffer.getInt();
            final var unk2 = buffer.getInt();
            final var unk3 = buffer.getInt();
            final var unk4 = buffer.getInt();
            final var programType = factory.<RTTITypeEnum>find("EProgramType").valueOf(buffer.getInt());
            final var unk5 = buffer.getInt();
            final var unk6 = buffer.getInt();
            final var unk7 = buffer.getInt();
            final var unk8 = buffer.getInt();
            final var unk9 = buffer.getInt();
            final var data = BufferUtils.getBytes(buffer, buffer.getInt());

            final var object = new ProgramEntry();
            object.data = data;
            object.programType = programType;

            return new RTTIObject(factory.find(ProgramEntry.class), object);
        }
    }
}
