package com.shade.decima.ui.data.viewer.texture.reader;

import be.twofold.tinybcdec.BlockDecoder;
import be.twofold.tinybcdec.BlockFormat;
import be.twofold.tinybcdec.PixelOrder;
import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.Set;

public class ImageReaderBC5 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderBC5();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC5U");
        }

        @NotNull
        @Override
        public Set<Channel> channels(@NotNull String format) {
            return EnumSet.of(Channel.R, Channel.G);
        }
    }

    public ImageReaderBC5() {
        super(8, 4, CM_INT_RGB);
    }

    @NotNull
    @Override
    public BufferedImage read(@NotNull ByteBuffer buffer, int width, int height) {
        var decoder = BlockDecoder.create(BlockFormat.BC5Unsigned, PixelOrder.of(4, 2, 1, 0, -1));
        var decoded = ByteBuffer.allocate(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);

        decoder.decode(width, height, buffer.array(), buffer.arrayOffset(), decoded.array(), decoded.arrayOffset());

        var image = createImage(width, height);
        var data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        decoded.asIntBuffer().get(data);

        return image;
    }
}
