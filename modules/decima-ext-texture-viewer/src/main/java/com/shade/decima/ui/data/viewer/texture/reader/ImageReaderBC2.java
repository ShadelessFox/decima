package com.shade.decima.ui.data.viewer.texture.reader;

import be.twofold.tinybcdec.BlockDecoder;
import be.twofold.tinybcdec.BlockFormat;
import be.twofold.tinybcdec.PixelOrder;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

public class ImageReaderBC2 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderBC2();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC2");
        }
    }

    public ImageReaderBC2() {
        super(8, 4, CM_INT_ARGB);
    }

    @NotNull
    @Override
    public BufferedImage read(@NotNull ByteBuffer buffer, int width, int height) {
        var decoder = BlockDecoder.create(BlockFormat.BC2, PixelOrder.ARGB);
        var decoded = ByteBuffer.allocate(width * height * 4);

        decoder.decode(width, height, buffer.array(), buffer.arrayOffset(), decoded.array(), decoded.arrayOffset());

        var image = createImage(width, height);
        var data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        decoded.asIntBuffer().get(data);

        return image;
    }
}
