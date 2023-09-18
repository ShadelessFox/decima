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

    private static final int DXGI_FORMAT_R32G32B32A32_FLOAT = 2;
    private static final int DXGI_FORMAT_R32G32B32A32_UINT = 3;
    private static final int DXGI_FORMAT_R32G32B32A32_SINT = 4;
    private static final int DXGI_FORMAT_R32G32B32_FLOAT = 6;
    private static final int DXGI_FORMAT_R16G16B16A16_FLOAT = 10;
    private static final int DXGI_FORMAT_R16G16B16A16_UNORM = 11;
    private static final int DXGI_FORMAT_R16G16B16A16_UINT = 12;
    private static final int DXGI_FORMAT_R16G16B16A16_SNORM = 13;
    private static final int DXGI_FORMAT_R16G16B16A16_SINT = 14;
    private static final int DXGI_FORMAT_R32G32_FLOAT = 16;
    private static final int DXGI_FORMAT_R32G32_UINT = 17;
    private static final int DXGI_FORMAT_R32G32_SINT = 18;
    private static final int DXGI_FORMAT_R10G10B10A2_UNORM = 24;
    private static final int DXGI_FORMAT_R11G11B10_FLOAT = 26;
    private static final int DXGI_FORMAT_R8G8B8A8_UNORM = 28;
    private static final int DXGI_FORMAT_R8G8B8A8_UINT = 30;
    private static final int DXGI_FORMAT_R8G8B8A8_SNORM = 31;
    private static final int DXGI_FORMAT_R8G8B8A8_SINT = 32;
    private static final int DXGI_FORMAT_R16G16_FLOAT = 34;
    private static final int DXGI_FORMAT_R16G16_UNORM = 35;
    private static final int DXGI_FORMAT_R16G16_UINT = 36;
    private static final int DXGI_FORMAT_R16G16_SNORM = 37;
    private static final int DXGI_FORMAT_R16G16_SINT = 38;
    private static final int DXGI_FORMAT_R32_FLOAT = 41;
    private static final int DXGI_FORMAT_R32_UINT = 42;
    private static final int DXGI_FORMAT_R32_SINT = 43;
    private static final int DXGI_FORMAT_R8G8_UINT = 50;
    private static final int DXGI_FORMAT_R8G8_SNORM = 51;
    private static final int DXGI_FORMAT_R8G8_SINT = 52;
    private static final int DXGI_FORMAT_R16_FLOAT = 54;
    private static final int DXGI_FORMAT_R16_UNORM = 56;
    private static final int DXGI_FORMAT_R16_UINT = 57;
    private static final int DXGI_FORMAT_R16_SNORM = 58;
    private static final int DXGI_FORMAT_R16_SINT = 59;
    private static final int DXGI_FORMAT_R8_UNORM = 61;
    private static final int DXGI_FORMAT_R8_UINT = 62;
    private static final int DXGI_FORMAT_R8_SNORM = 63;
    private static final int DXGI_FORMAT_R8_SINT = 64;
    private static final int DXGI_FORMAT_R8G8_B8G8_UNORM = 68;
    private static final int DXGI_FORMAT_BC1_UNORM = 71;
    private static final int DXGI_FORMAT_BC2_UNORM = 74;
    private static final int DXGI_FORMAT_BC3_UNORM = 77;
    private static final int DXGI_FORMAT_BC4_UNORM = 80;
    private static final int DXGI_FORMAT_BC4_SNORM = 81;
    private static final int DXGI_FORMAT_BC5_UNORM = 83;
    private static final int DXGI_FORMAT_BC5_SNORM = 84;
    private static final int DXGI_FORMAT_BC6H_UF16 = 95;
    private static final int DXGI_FORMAT_BC6H_SF16 = 96;
    private static final int DXGI_FORMAT_BC7_UNORM = 98;

    private static final Map<String, Integer> DXGI_MAPPINGS = Map.<String, Integer>ofEntries(
        Map.entry("RGBA_8888", DXGI_FORMAT_R8G8B8A8_UNORM),
        Map.entry("RGBA_FLOAT_32", DXGI_FORMAT_R32G32B32A32_FLOAT),
        Map.entry("RGB_FLOAT_32", DXGI_FORMAT_R32G32B32_FLOAT),
        Map.entry("RG_FLOAT_32", DXGI_FORMAT_R32G32_FLOAT),
        Map.entry("R_FLOAT_32", DXGI_FORMAT_R32_FLOAT),
        Map.entry("RGBA_FLOAT_16", DXGI_FORMAT_R16G16B16A16_FLOAT),
        Map.entry("RGB_FLOAT_16", DXGI_FORMAT_R16G16B16A16_FLOAT),
        Map.entry("RG_FLOAT_16", DXGI_FORMAT_R16G16_FLOAT),
        Map.entry("R_FLOAT_16", DXGI_FORMAT_R16_FLOAT),
        Map.entry("RGBA_UNORM_32", DXGI_FORMAT_R32G32B32A32_UINT),
        Map.entry("RGBA_UNORM_16", DXGI_FORMAT_R16G16B16A16_UNORM),
        Map.entry("RG_UNORM_16", DXGI_FORMAT_R16G16_UNORM),
        Map.entry("R_UNORM_16", DXGI_FORMAT_R16_UNORM),
        Map.entry("RGBA_UNORM_8", DXGI_FORMAT_R8G8B8A8_UNORM),
        Map.entry("RG_UNORM_8", DXGI_FORMAT_R8G8_B8G8_UNORM),
        Map.entry("R_UNORM_8", DXGI_FORMAT_R8_UNORM),
        Map.entry("RGBA_NORM_16", DXGI_FORMAT_R16G16B16A16_SNORM),
        Map.entry("RG_NORM_16", DXGI_FORMAT_R16G16_SNORM),
        Map.entry("R_NORM_16", DXGI_FORMAT_R16_SNORM),
        Map.entry("RGBA_NORM_8", DXGI_FORMAT_R8G8B8A8_SNORM),
        Map.entry("RG_NORM_8", DXGI_FORMAT_R8G8_SNORM),
        Map.entry("R_NORM_8", DXGI_FORMAT_R8_SNORM),
        Map.entry("RGBA_UINT_32", DXGI_FORMAT_R32G32B32A32_UINT),
        Map.entry("RG_UINT_32", DXGI_FORMAT_R32G32_UINT),
        Map.entry("R_UINT_32", DXGI_FORMAT_R32_UINT),
        Map.entry("RGBA_UINT_16", DXGI_FORMAT_R16G16B16A16_UINT),
        Map.entry("RG_UINT_16", DXGI_FORMAT_R16G16_UINT),
        Map.entry("R_UINT_16", DXGI_FORMAT_R16_UINT),
        Map.entry("RGBA_UINT_8", DXGI_FORMAT_R8G8B8A8_UINT),
        Map.entry("RG_UINT_8", DXGI_FORMAT_R8G8_UINT),
        Map.entry("R_UINT_8", DXGI_FORMAT_R8_UINT),
        Map.entry("RGBA_INT_32", DXGI_FORMAT_R32G32B32A32_SINT),
        Map.entry("RG_INT_32", DXGI_FORMAT_R32G32_SINT),
        Map.entry("R_INT_32", DXGI_FORMAT_R32_SINT),
        Map.entry("RGBA_INT_16", DXGI_FORMAT_R16G16B16A16_SINT),
        Map.entry("RG_INT_16", DXGI_FORMAT_R16G16_SINT),
        Map.entry("R_INT_16", DXGI_FORMAT_R16_SINT),
        Map.entry("RGBA_INT_8", DXGI_FORMAT_R8G8B8A8_SINT),
        Map.entry("RG_INT_8", DXGI_FORMAT_R8G8_SINT),
        Map.entry("R_INT_8", DXGI_FORMAT_R8_SINT),
        Map.entry("RGB_FLOAT_11_11_10", DXGI_FORMAT_R11G11B10_FLOAT),
        Map.entry("RGBA_UNORM_10_10_10_2", DXGI_FORMAT_R10G10B10A2_UNORM),
        Map.entry("BC1", DXGI_FORMAT_BC1_UNORM),
        Map.entry("BC2", DXGI_FORMAT_BC2_UNORM),
        Map.entry("BC3", DXGI_FORMAT_BC3_UNORM),
        Map.entry("BC4U", DXGI_FORMAT_BC4_UNORM),
        Map.entry("BC4S", DXGI_FORMAT_BC4_SNORM),
        Map.entry("BC5U", DXGI_FORMAT_BC5_UNORM),
        Map.entry("BC5S", DXGI_FORMAT_BC5_SNORM),
        Map.entry("BC6U", DXGI_FORMAT_BC6H_UF16),
        Map.entry("BC6S", DXGI_FORMAT_BC6H_SF16),
        Map.entry("BC7", DXGI_FORMAT_BC7_UNORM)
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
