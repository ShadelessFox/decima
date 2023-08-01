package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "ShaderResource", game = GameType.DS),
    @Type(name = "ShaderResource", game = GameType.DSDC)
})
public class DSShaderHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final var size = buffer.getInt();
        final var hash = registry.find("MurmurHashValue").read(registry, buffer);
        final var unk1 = buffer.getInt();
        final var programTypeMask = registry.<RTTITypeEnum>find("EProgramTypeMask").valueOf(buffer.getInt());
        final var unk2 = buffer.getInt();
        final var programs = BufferUtils.getObjects(buffer, buffer.getInt(), RTTIObject[]::new, buf -> ProgramEntry.read(registry, buf));
        final var rootSignature = BufferUtils.getBytes(buffer, buffer.getInt());

        object.set("Hash", hash);
        object.set("Unk1", unk1);
        object.set("ProgramTypeMask", programTypeMask);
        object.set("Unk2", unk2);
        object.set("Programs", programs);
        object.set("RootSignature", rootSignature);
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
            new Component("Hash", registry.find("MurmurHashValue")),
            new Component("Unk1", registry.find("uint32")),
            new Component("ProgramTypeMask", registry.find("EProgramTypeMask")),
            new Component("Unk2", registry.find("uint32")),
            new Component("Programs", registry.find(ProgramEntry[].class)),
            new Component("RootSignature", registry.find("Array<uint8>"))
        };
    }

    public static class ProgramEntry {
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] data;
        @RTTIField(type = @Type(name = "EProgramType"))
        public RTTITypeEnum.Constant programType;
        @RTTIField(type = @Type(name = "uint"))
        public int shaderModel;

        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var unk0 = buffer.getInt();
            final var unk1 = buffer.getInt();
            final var unk2 = buffer.getInt();
            final var unk3 = buffer.getInt();
            final var unk4 = buffer.getInt();
            final var programType = registry.<RTTITypeEnum>find("EProgramType").valueOf(buffer.getInt());
            final var shaderModel = buffer.getInt();
            final var unk5 = buffer.getInt();
            final var unk6 = buffer.getInt();
            final var unk7 = buffer.getInt();
            final var unk8 = buffer.getInt();
            final var unk9 = buffer.getInt();
            final var data = BufferUtils.getBytes(buffer, buffer.getInt());

            final var object = new ProgramEntry();
            object.data = data;
            object.programType = programType;
            object.shaderModel = shaderModel;

            return new RTTIObject(registry.find(ProgramEntry.class), object);
        }
    }
}
