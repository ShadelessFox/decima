package com.shade.decima.model.rtti.messages.impl;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.java.RTTIExtends;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.decima.ui.data.registry.Type;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.StringJoiner;
import java.util.function.IntSupplier;

@MessageHandlerRegistration(type = "ShaderResource", message = "MsgReadBinary", game = GameType.DS)
public class ShaderResource implements MessageHandler.ReadBinary {

    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final var gguuid = registry.find("GGUUID");
        object.set("TotalSize", buffer.getInt());
        object.set("UnkGuid", gguuid.read(registry, buffer));
        buffer.position(buffer.position() + 12);
        final int shaderCount = buffer.getInt();
        RTTIObject[] shaders = new RTTIObject[shaderCount];
        for (int i = 0; i < shaderCount; i++) {
            RTTIObject shader = ShaderEntry.read(registry, buffer);
            shaders[i] = shader;
        }

        object.set("ShaderEntries", shaders);
        int unkDataSize = buffer.getInt();
        byte[] unkData = IOUtils.getBytesExact(buffer, unkDataSize);
        object.set("UnkData", unkData);
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
            new Component("TotalSize", registry.find("uint32")),
            new Component("UnkGuid", registry.find("GGUUID")),
            new Component("ShaderEntries", registry.find(ShaderEntry[].class)),
            new Component("UnkData", registry.find("Array<uint8>")),
        };
    }

    public static class DXBCShader {
        public static class Chunk {
            @RTTIField(type = @Type(name = "String"))
            public Object name;
            @RTTIField(type = @Type(name = "uint32"))
            public int size;
//            @RTTIField(type = @Type(name = "Array<uint8>"))
//            public byte[] data;

            public Chunk(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
                name = IOUtils.getString(buffer, 4);
                size = buffer.getInt();


            }

            @NotNull
            public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
                final var object = new Chunk(registry, buffer);
                IOUtils.getBytesExact(buffer, object.size);
                return new RTTIObject(registry.find(Chunk.class), object);
            }
        }

        @RTTIExtends(@Type(type = Chunk.class))
        public static class RDEFChunk extends Chunk {
            public enum ShaderFlags implements IntSupplier {
                None(0),
                Debug(1),
                SkipValidation(2),
                SkipOptimization(4),
                PackMatrixRowMajor(8),
                PackMatrixColumnMajor(16),
                PartialPrecision(32),
                ForceVsSoftwareNoOpt(64),
                ForcePsSoftwareNoOpt(128),
                NoPreshader(256),
                AvoidFlowControl(512),
                PreferFlowControl(1024),
                EnableStrictness(2048),
                EnableBackwardsCompatibility(4096),
                IeeeStrictness(8192),
                OptimizationLevel0(16384),
                OptimizationLevel1(0),
                OptimizationLevel2(49152),
                OptimizationLevel3(32768),
                Reserved16(65536),
                Reserved17(131072),
                WarningsAreErrors(262144);

                private final int key;

                ShaderFlags(int key) {
                    this.key = key;
                }


                @Override
                public int getAsInt() {
                    return key;
                }
            }

            public enum ProgramType implements IntSupplier {
                VERTEX(0xFFFE),
                FRAGMENT(0xFFFF);

                final int key;

                ProgramType(int key) {
                    this.key = key;
                }

                @Override
                public int getAsInt() {
                    return key;
                }
            }

            public static class ConstantBuffer {
                public static class Constant {
                    public enum VariableFlags implements IntSupplier {
                        D3D_SVF_USERPACKED(1),
                        D3D_SVF_USED(2),
                        D3D_SVF_INTERFACE_POINTER(4),
                        D3D_SVF_INTERFACE_PARAMETER(8),
                        D3D10_SVF_USERPACKED(16),
                        D3D10_SVF_USED(32),
                        D3D11_SVF_INTERFACE_POINTER(64),
                        D3D11_SVF_INTERFACE_PARAMETER(128);

                        public final int key;

                        VariableFlags(int key) {
                            this.key = key;
                        }

                        @Override
                        public int getAsInt() {
                            return key;
                        }
                    }


                    @RTTIField(type = @Type(name = "String"))
                    public String name;
                    @RTTIField(type = @Type(name = "Array<uint8>"))
                    public byte[] data;
                    @RTTIField(type = @Type(name = "String"))
                    public String flags;

                    public Constant(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, int chunkStart) {
                        int nameOffset = buffer.getInt();
                        int dataOffset = buffer.getInt();
                        int varSize = buffer.getInt();
                        int varFlags = buffer.getInt();
                        int varTypeNameOffset = buffer.getInt();
                        int defaultValueOffset = buffer.getInt();
                        int dataEnd = buffer.position();

                        buffer.position(nameOffset + chunkStart);
                        name = IOUtils.getNullTerminatedString(buffer);
                        if (dataOffset > 0) {
                            buffer.position(nameOffset + dataOffset);
                            data = IOUtils.getBytesExact(buffer, varSize);
                        }
                        flags = flagToString(VariableFlags.class, varFlags);
                        buffer.position(dataEnd);
                    }

                    @NotNull
                    public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, int chunkStart) {
                        final var object = new Constant(registry, buffer, chunkStart);
                        return new RTTIObject(registry.find(Constant.class), object);
                    }
                }

                public enum ConstantBufferType implements IntSupplier {
                    CBUFFER(0),
                    TBUFFER(1),
                    INTERFACE_POINTERS(2);

                    public final int key;

                    ConstantBufferType(int key) {
                        this.key = key;
                    }

                    @Override
                    public int getAsInt() {
                        return key;
                    }
                }

                @RTTIField(type = @Type(name = "String"))
                public String name;
                @RTTIField(type = @Type(name = "uint32"))
                public int flags;
                @RTTIField(type = @Type(name = "String"))
                public String bufferType;
                @RTTIField(type = @Type(type = Constant[].class))
                public RTTIObject[] variables;

                public ConstantBuffer(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, int chunkStart) {
                    int start = buffer.position();
                    int nameOffset = buffer.getInt();
                    int variableCount = buffer.getInt();
                    int varDescOffset = buffer.getInt();
                    int bufferSize = buffer.getInt();
                    flags = buffer.getInt();
                    bufferType = flagToString(ConstantBufferType.class, buffer.getInt());

                    buffer.position(chunkStart + nameOffset);
                    name = IOUtils.getNullTerminatedString(buffer);

                    buffer.position(chunkStart + varDescOffset);
                    variables = new RTTIObject[variableCount];
                    for (int i = 0; i < variableCount; i++) {
                        variables[i] = Constant.read(registry, buffer, chunkStart);
                    }

                    buffer.position(start + 24);
                }

                @NotNull
                public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, int chunkStart) {
                    final var object = new ConstantBuffer(registry, buffer, chunkStart);
                    return new RTTIObject(registry.find(ConstantBuffer.class), object);
                }
            }

            public static class ResourceBinding {
                @RTTIField(type = @Type(name = "String"))
                public String name;

                public ResourceBinding(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, int chunkStart) {
                    int start = buffer.position();
                    int nameOffset = buffer.getInt();
                    buffer.position(chunkStart + nameOffset);
                    name = IOUtils.getNullTerminatedString(buffer);

                    buffer.position(start + 40);

                }

                @NotNull
                public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, int chunkStart) {
                    final var object = new ResourceBinding(registry, buffer, chunkStart);
                    return new RTTIObject(registry.find(ResourceBinding.class), object);
                }


            }

            @RTTIField(type = @Type(name = "String"))
            public String version;
            @RTTIField(type = @Type(name = "String"))
            public String programType;
            @RTTIField(type = @Type(name = "String"))
            public String shaderFlags;
            @RTTIField(type = @Type(name = "String"))
            public String compiler;
            @RTTIField(type = @Type(type = ConstantBuffer[].class))
            public RTTIObject[] constantBuffers;

            @RTTIField(type = @Type(type = ResourceBinding[].class))
            public RTTIObject[] resourceBindings;

            public RDEFChunk(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
                super(registry, buffer);
                int chunkStart = buffer.position();
                int constantBufferCount = buffer.getInt();
                int constantBufferOffset = buffer.getInt();
                int resourceBindingCount = buffer.getInt();
                int resourceBindingOffset = buffer.getInt();
                version = buffer.get() + "." + buffer.get();
                programType = enumToString(ProgramType.class, buffer.getShort() & 0xFFFF);
                shaderFlags = flagToString(ShaderFlags.class, buffer.getInt());
                int creatorOffset = buffer.getInt();
                if (creatorOffset > 0) {
                    buffer.position(chunkStart + creatorOffset);
                    compiler = IOUtils.getNullTerminatedString(buffer);
                }
                buffer.position(chunkStart + constantBufferOffset);
                constantBuffers = new RTTIObject[constantBufferCount];
                for (int i = 0; i < constantBufferCount; i++) {
                    RTTIObject constantBuffer = ConstantBuffer.read(registry, buffer, chunkStart);
                    constantBuffers[i] = constantBuffer;
                }

                buffer.position(chunkStart + resourceBindingOffset);
                resourceBindings = new RTTIObject[resourceBindingCount];
                for (int i = 0; i < resourceBindingCount; i++) {
                    RTTIObject resource = ResourceBinding.read(registry, buffer, chunkStart);
                    resourceBindings[i] = resource;
                }


            }

            @NotNull
            public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
                final var object = new RDEFChunk(registry, buffer);
                return new RTTIObject(registry.find(RDEFChunk.class), object);
            }
        }

        @RTTIField(type = @Type(name = "GGUUID"))
        public Object hash;
        @RTTIField(type = @Type(type = Chunk[].class))
        public RTTIObject[] chunks;


        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var gguuid = registry.find("GGUUID");
            final var object = new DXBCShader();
            final int chunkStart = buffer.position();
            String ident = IOUtils.getString(buffer, 4);
            if (!ident.equals("DXBC")) {
                throw new IllegalStateException("Invalid DXBC ident, got %s".formatted(ident));
            }
            object.hash = gguuid.read(registry, buffer);
            final int tmp = buffer.getInt();
            if (tmp != 1) {
                throw new IllegalStateException("Invalid const 1");
            }
            buffer.getInt();
            final int chunkCount = buffer.getInt();
            final int[] chunkOffsets = new int[chunkCount];
            for (int i = 0; i < chunkCount; i++) {
                chunkOffsets[i] = buffer.getInt();
            }
            object.chunks = new RTTIObject[chunkCount];
            for (int i = 0; i < chunkCount; i++) {
                buffer.position(chunkStart + chunkOffsets[i]);
                String chunkType = IOUtils.getString(buffer, 4);
                buffer.position(chunkStart + chunkOffsets[i]);
                RTTIObject chunk;
                if (chunkType.equals("RDEF")) {
                    chunk = RDEFChunk.read(registry, buffer);
                } else {
                    chunk = Chunk.read(registry, buffer);
                }
                object.chunks[i] = chunk;

            }
            return new RTTIObject(registry.find(DXBCShader.class), object);
        }
    }

    public static class ShaderEntry {
        @RTTIField(type = @Type(name = "Array<uint8>"))
        public byte[] unk0;
        @RTTIField(type = @Type(name = "GGUUID"))
        public Object unk1;
        @RTTIField(type = @Type(type = DXBCShader.class))
        public RTTIObject shader;


        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final var gguuid = registry.find("GGUUID");
            final var object = new ShaderEntry();
            object.unk0 = IOUtils.getBytesExact(buffer, 32);
            object.unk1 = gguuid.read(registry, buffer);
            buffer.getInt();
            object.shader = DXBCShader.read(registry, buffer);

            return new RTTIObject(registry.find(ShaderEntry.class), object);
        }
    }

    public static <T extends Enum<T> & IntSupplier> String flagToString(Class<T> flagClass, int value) {
        StringJoiner sj = new StringJoiner("|");
        for (T constant : flagClass.getEnumConstants()) {
            if ((value & constant.getAsInt()) != 0) {
                sj.add(constant.name());
            }
        }
        return sj.toString();
    }

    public static <T extends Enum<T> & IntSupplier> String enumToString(Class<T> enumClass, int value) {
        for (T constant : enumClass.getEnumConstants()) {
            if (value == constant.getAsInt()) {
                return constant.name();
            }
        }
        return null;
    }
}
