package com.shade.decima.ui.data.viewer.texture.exporter;

import com.shade.decima.ui.data.viewer.texture.TextureExporter;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.util.NotNull;

import java.awt.image.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Set;

public class TextureExporterTGA implements TextureExporter {
    @Override
    public void export(@NotNull ImageProvider provider, @NotNull Set<Option> options, @NotNull WritableByteChannel channel) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
        BufferedImage image = provider.getImage(0, 0);
        int bitsPerPixel = image.getColorModel().getPixelSize();

        header.put((byte) 0); // idlength
        header.put((byte) 0); // colourmaptype
        header.put((byte) (bitsPerPixel == 8 ? 3 : 2)); // datatypecode
        header.putShort((short) 0); // colourmaporigin
        header.putShort((short) 0); // colourmaplength
        header.put((byte) 0); // colourmapdepth
        header.putShort((short) 0); // x_origin
        header.putShort((short) 0); // y_origin
        header.putShort((short) image.getWidth());//width
        header.putShort((short) image.getHeight());//height
        header.put((byte) bitsPerPixel);//bitsperpixel
        header.put((byte) 0);//imagedescriptor

        channel.write(header.position(0));

        ByteBuffer byteBuffer;

        DataBuffer dataBuffer = provider.getImage(0, 0).getData().getDataBuffer();

        if (dataBuffer instanceof DataBufferByte) {
            if (bitsPerPixel != 8) {
                throw new IllegalStateException("Expected 8 bits per pixel for Byte data buffer");
            }
            byte[] pixelData = ((DataBufferByte) dataBuffer).getData();
            byteBuffer = ByteBuffer.wrap(pixelData);
        } else if (dataBuffer instanceof DataBufferUShort) {
            if (bitsPerPixel != 16) {
                throw new IllegalStateException("Expected 16 bits per pixel for Short data buffer");
            }
            short[] pixelData = ((DataBufferUShort) dataBuffer).getData();
            byteBuffer = ByteBuffer.allocate(pixelData.length * 2);
            byteBuffer.asShortBuffer().put(ShortBuffer.wrap(pixelData));
        } else if (dataBuffer instanceof DataBufferShort) {
            if (bitsPerPixel != 16) {
                throw new IllegalStateException("Expected 16 bits per pixel for Short data buffer");
            }
            short[] pixelData = ((DataBufferShort) dataBuffer).getData();
            byteBuffer = ByteBuffer.allocate(pixelData.length * 2);
            byteBuffer.asShortBuffer().put(ShortBuffer.wrap(pixelData));
        } else if (dataBuffer instanceof DataBufferInt) {
            if (bitsPerPixel != 32 && bitsPerPixel != 24) {
                throw new IllegalStateException("Expected 32/24 bits per pixel for Int data buffer");
            }
            int[] pixelData = ((DataBufferInt) dataBuffer).getData();
            final int bandCount = bitsPerPixel / 8;
            byteBuffer = ByteBuffer.allocate(pixelData.length * bandCount);
            int width = image.getWidth();
            int height = image.getHeight();
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int val = pixelData[y + height * (width-x-1)];
                    for (int b = 0; b < bandCount; b++) {
                        byteBuffer.put((byte) (val >>> (b * 8)));
                    }
                }
            }

        } else {
            throw new IllegalArgumentException("Not implemented for data buffer type: " + dataBuffer.getClass());
        }
        channel.write(byteBuffer.position(0));

        ByteBuffer footer = ByteBuffer.allocate(26).order(ByteOrder.LITTLE_ENDIAN);
        footer.putInt(0);
        footer.putInt(0);
        footer.put("TRUEVISION-XFILE.\0".getBytes());

        channel.write(footer.position(0));
    }

    @Override
    public boolean supportsImage(@NotNull ImageProvider provider) {
        return provider.getType() == ImageProvider.Type.TEXTURE;
    }

    @Override
    public boolean supportsOption(@NotNull Option option) {
        return false;
    }

    @NotNull
    @Override
    public String getExtension() {
        return "tga";
    }
}
