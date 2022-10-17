package com.shade.decima.ui.data.viewer.texture.exporter;

import com.shade.decima.ui.data.viewer.texture.TextureExporter;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.Set;

public class TextureExporterDDS implements TextureExporter {
    private static final int DDS_MAGIC = 'D' | 'D' << 8 | 'S' << 16 | ' ' << 24;

    private static final int DDSD_CAPS = 0x1;
    private static final int DDSD_HEIGHT = 0x2;
    private static final int DDSD_WIDTH = 0x4;
    private static final int DDSD_PIXELFORMAT = 0x1000;
    private static final int DDSD_MIPMAPCOUNT = 0x20000;
    private static final int DDSD_DEPTH = 0x800000;

    private static final int DDSCAPS_COMPLEX = 0x8;
    private static final int DDSCAPS_MIPMAP = 0x400000;
    private static final int DDSCAPS_TEXTURE = 0x1000;

    private static final int DDSCAPS2_CUBEMAP = 0x200;
    private static final int DDSCAPS2_CUBEMAP_POSITIVEX = 0x400;
    private static final int DDSCAPS2_CUBEMAP_NEGATIVEX = 0x800;
    private static final int DDSCAPS2_CUBEMAP_POSITIVEY = 0x1000;
    private static final int DDSCAPS2_CUBEMAP_NEGATIVEY = 0x2000;
    private static final int DDSCAPS2_CUBEMAP_POSITIVEZ = 0x4000;
    private static final int DDSCAPS2_CUBEMAP_NEGATIVEZ = 0x8000;
    private static final int DDSCAPS2_VOLUME = 0x200000;

    private static final int DDPF_FOURCC = 0x4;
    private static final int DDPF_FOURCC_DX10 = 'D' | 'X' << 8 | '1' << 16 | '0' << 24;

    private static final int DDS_DIMENSION_TEXTURE2D = 3;
    private static final int DDS_DIMENSION_TEXTURE3D = 4;
    private static final int DDS_RESOURCE_MISC_TEXTURECUBE = 0x4;

    // Only seen formats are listed here. No urge to add all other formats now
    private static final Map<String, Integer> DXGI_MAPPINGS = Map.ofEntries(
        Map.entry("RGBA_8888", 28),
        Map.entry("BC1", 71),
        Map.entry("BC2", 74),
        Map.entry("BC3", 77),
        Map.entry("BC4U", 80),
        Map.entry("BC4S", 81),
        Map.entry("BC5U", 83),
        Map.entry("BC5S", 84),
        Map.entry("BC6U", 95),
        Map.entry("BC6S", 96),
        Map.entry("BC7", 98)
    );

    @Override
    public void export(@NotNull ImageProvider provider, @NotNull Set<Option> options, @NotNull WritableByteChannel channel) throws IOException {
        writeHeader(provider, channel);
        writeData(provider, channel);
    }

    @Override
    public boolean supportsImage(@NotNull ImageProvider provider) {
        return DXGI_MAPPINGS.containsKey(provider.getPixelFormat());
    }

    @Override
    public boolean supportsOption(@NotNull Option option) {
        return false;
    }

    @NotNull
    @Override
    public String getExtension() {
        return "dds";
    }

    private void writeHeader(@NotNull ImageProvider provider, @NotNull WritableByteChannel channel) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(148).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(DDS_MAGIC);                 /* dwMagic */
        buffer.putInt(124);                       /* dwSize */
        buffer.putInt(computeDDSFlags(provider)); /* dwFlags */
        buffer.putInt(provider.getMaxHeight());   /* dwHeight */
        buffer.putInt(provider.getMaxWidth());    /* dwWidth */
        buffer.putInt(0);                         /* dwPitchOrLinearSize */
        buffer.putInt(provider.getDepth());       /* dwDepth */
        buffer.putInt(provider.getMipCount());    /* dwMipMapCount */
        buffer.position(buffer.position() + 44);  /* dwReserved1 */

        { // ddsPixelFormat
            buffer.putInt(32);               /* dwSize */
            buffer.putInt(DDPF_FOURCC);      /* dwFlags */
            buffer.putInt(DDPF_FOURCC_DX10); /* dwFourCC */
            buffer.putInt(0);                /* dwRGBBitCount */
            buffer.putInt(0);                /* dwRBitMask */
            buffer.putInt(0);                /* dwGBitMask */
            buffer.putInt(0);                /* dwBBitMask */
            buffer.putInt(0);                /* dwABitMask */
        }

        buffer.putInt(computeCapsFlags(provider));  /* dwCaps */
        buffer.putInt(computeCaps2Flags(provider)); /* dwCaps2 */
        buffer.putInt(0);                           /* dwCaps3 */
        buffer.putInt(0);                           /* dwCaps4 */
        buffer.position(buffer.position() + 4);     /* dwReserved2 */

        { // dx10Header
            final int format = DXGI_MAPPINGS.get(provider.getPixelFormat());
            final int dimension = provider.getType() == ImageProvider.Type.VOLUME
                ? DDS_DIMENSION_TEXTURE3D
                : DDS_DIMENSION_TEXTURE2D;
            final int miscFlags = provider.getType() == ImageProvider.Type.CUBEMAP
                ? DDS_RESOURCE_MISC_TEXTURECUBE
                : 0;

            buffer.putInt(format);                  /* dxgiFormat */
            buffer.putInt(dimension);               /* resourceDimension */
            buffer.putInt(miscFlags);               /* miscFlag */
            buffer.putInt(provider.getArraySize()); /* arraySize */
            buffer.putInt(0);                       /* miscFlags2 */
        }

        buffer.position(0);
        channel.write(buffer);
    }

    private void writeData(@NotNull ImageProvider provider, @NotNull WritableByteChannel channel) throws IOException {
        if (provider.getType() == ImageProvider.Type.VOLUME) {
            for (int mip = 0; mip < provider.getMipCount(); mip++) {
                for (int slice = 0; slice < provider.getSliceCount(mip); slice++) {
                    channel.write(provider.getData(mip, slice));
                }
            }
        } else {
            for (int slice = 0; slice < provider.getSliceCount(0); slice++) {
                for (int mip = 0; mip < provider.getMipCount(); mip++) {
                    channel.write(provider.getData(mip, slice));
                }
            }
        }
    }

    private static int computeDDSFlags(@NotNull ImageProvider provider) {
        int flags = DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT;

        if (provider.getMipCount() > 1) {
            flags |= DDSD_MIPMAPCOUNT;
        }

        if (provider.getType() == ImageProvider.Type.VOLUME) {
            flags |= DDSD_DEPTH;
        }

        return flags;
    }

    private static int computeCapsFlags(@NotNull ImageProvider provider) {
        int flags = DDSCAPS_TEXTURE;

        if (provider.getMipCount() > 1) {
            flags |= DDSCAPS_MIPMAP;
        }

        if (provider.getMipCount() > 0 || provider.getArraySize() > 0 || provider.getDepth() > 0) {
            flags |= DDSCAPS_COMPLEX;
        }

        return flags;
    }

    private static int computeCaps2Flags(@NotNull ImageProvider provider) {
        int flags = 0;

        if (provider.getType() == ImageProvider.Type.CUBEMAP) {
            flags |= DDSCAPS2_CUBEMAP;
            flags |= DDSCAPS2_CUBEMAP_POSITIVEX | DDSCAPS2_CUBEMAP_NEGATIVEX;
            flags |= DDSCAPS2_CUBEMAP_POSITIVEY | DDSCAPS2_CUBEMAP_NEGATIVEY;
            flags |= DDSCAPS2_CUBEMAP_POSITIVEZ | DDSCAPS2_CUBEMAP_NEGATIVEZ;
        } else if (provider.getType() == ImageProvider.Type.VOLUME) {
            flags |= DDSCAPS2_VOLUME;
        }

        return flags;
    }
}
