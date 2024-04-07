package com.shade.decima.hfw.rtti.types;

import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.java.HwType;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public class HFWTextureFrames implements HwType {
    public record Span(int offset, int size) {}

    private byte[] data;
    private Span[] spans;
    private int width;
    private int height;
    private RTTIEnum.Constant format;
    private RTTIEnum.Constant type;
    private int size;
    private float unkFloat1;
    private float unkFloat2;

    @NotNull
    public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final HFWTextureFrames object = new HFWTextureFrames();

        object.data = BufferUtils.getBytes(buffer, buffer.getInt());
        object.spans = BufferUtils.getObjects(buffer, buffer.getInt(), Span[]::new, buf -> new Span(buf.getInt(), buf.getInt()));
        object.width = buffer.getInt();
        object.height = buffer.getInt();
        object.format = factory.<RTTIEnum>find("EPixelFormat").valueOf(buffer.getInt());
        object.type = factory.<RTTIEnum>find("ETextureType").valueOf(buffer.getInt());
        object.size = buffer.getInt(); // allocation size; dimensions are aligned for compressed textures
        object.unkFloat1 = buffer.getFloat();
        object.unkFloat2 = buffer.getFloat();

        return new RTTIObject(factory.find(HFWTextureFrames.class), object);
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize() {
        throw new NotImplementedException();
    }
}
