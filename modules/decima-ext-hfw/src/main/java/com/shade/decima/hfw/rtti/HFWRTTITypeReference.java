package com.shade.decima.hfw.rtti;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.RTTIDefinition;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.RTTITypeReference;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

@RTTIDefinition(value = {"Ref", "cptr", "StreamingRef", "UUIDRef", "WeakPtr"}, game = GameType.HFW)
public class HFWRTTITypeReference extends RTTITypeReference {
    public HFWRTTITypeReference(@NotNull String name, @NotNull RTTIType<?> type) {
        super(name, type);
    }

    @NotNull
    @Override
    public RTTIReference read(@NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final byte type = buffer.get();
        return switch (type) {
            case 0 -> RTTIReference.NONE;
            case 1 -> {
                if (getTypeName().equals("UUIDRef")) {
                    yield new RTTIReference.Internal(RTTIReference.Kind.REFERENCE, factory.<RTTIType<RTTIObject>>find("GGUUID").read(factory, reader, buffer));
                } else {
                    yield new RTTIReference.StreamingLink(null);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported reference type: " + type);
        };
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIReference value) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTIFactory factory, @NotNull RTTIReference value) {
        throw new NotImplementedException();
    }
}
