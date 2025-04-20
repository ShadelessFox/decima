package com.shade.decima.ui.data.viewer.texture.reader;

import be.twofold.tinybcdec.BlockDecoder;
import be.twofold.tinybcdec.BlockFormat;
import be.twofold.tinybcdec.PixelOrder;
import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.Set;

public class ImageReaderBC6 extends ImageReader {
    public static class Provider implements ImageReaderProvider {
        @NotNull
        @Override
        public ImageReader create(@NotNull String format) {
            return new ImageReaderBC6(format.equals("BC6S"));
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC6U") || format.equals("BC6S");
        }

        @NotNull
        @Override
        public Set<Channel> channels(@NotNull String format) {
            return EnumSet.of(Channel.R, Channel.G, Channel.B);
        }
    }

    private final boolean signed;

    public ImageReaderBC6(boolean signed) {
        super(8, 4, CM_FLOAT_RGB);
        this.signed = signed;
    }

    @NotNull
    @Override
    public BufferedImage read(@NotNull ByteBuffer buffer, int width, int height) {
        var decoder = BlockDecoder.create(signed ? BlockFormat.BC6Signed : BlockFormat.BC6Unsigned, PixelOrder.RGB);
        var decoded = ByteBuffer.allocate(width * height * 4 * 2).order(ByteOrder.LITTLE_ENDIAN);

        decoder.decode(width, height, buffer.array(), buffer.arrayOffset(), decoded.array(), decoded.arrayOffset());

        var image = createImage(width, height);
        var raster = image.getRaster();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, BufferUtils.getHalfFloat(decoded));
                raster.setSample(x, y, 1, BufferUtils.getHalfFloat(decoded));
                raster.setSample(x, y, 2, BufferUtils.getHalfFloat(decoded));
            }
        }

        return image;
    }
}
