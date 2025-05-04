package com.shade.decima.game.killzone3.rtti.callbacks;

import com.shade.decima.game.killzone3.rtti.Killzone3.EPixelFormat;
import com.shade.decima.game.killzone3.rtti.data.ETextureType;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public class TextureCallback implements ExtraBinaryDataCallback<TextureCallback.TextureData> {
    public interface TextureData {
    }

    @Override
    public void deserialize(BinaryReader reader, TypeFactory factory, TextureData object) throws IOException {
        // Header
        var type = ETextureType.valueOf(reader.readByte());
        var width = 1 << reader.readByte();
        var height = 1 << reader.readByte();
        var depth = reader.readByte();
        var mips = reader.readByte();
        var format = EPixelFormat.valueOf(reader.readByte());
        var unk06 = reader.readByte();
        var unk07 = reader.readByte();
        var unk08 = reader.readByte();

        var totalSize = reader.readInt();
        var unk0D = reader.readInt();

        var streamedSize = 0;
        var streamedOffset = 0;
        var embeddedSize = totalSize;

        var mipmapsSize = gGetNumPixelsInMipmaps(format, width, height, mips - 1) * gPixelFormatBits(format) / 8;
        if (type == ETextureType._2D && mips > 1 && mipmapsSize >= 65536) {
            // TODO: Likely incorrect, need to also take in account 3D and CubeMap textures; see sub_4AB90
            int firstMipmapSize = gGetNumBytesInMipmaps(1, format, width, height);
            streamedOffset = reader.readInt();
            streamedSize = firstMipmapSize;
            embeddedSize -= streamedSize;
        }

        var embeddedData = reader.readBytes(embeddedSize);
    }

    private static int gGetNumBytesInMipmaps(int inNumMipMaps, EPixelFormat inPixelFormat, int inWidth, int inHeight) {
        boolean isCompressed = switch (inPixelFormat) {
            case S3TC1, S3TC3, S3TC5 -> true;
            default -> false;
        };
        int result = 0;
        if (inNumMipMaps > 0) {
            int index = inNumMipMaps;
            int currentMip = 0;
            int mipWidth;
            int mipHeight;
            do {
                mipWidth = inWidth >> currentMip;
                mipHeight = inHeight >> currentMip;
                if (isCompressed) {
                    mipHeight = (mipHeight + 3) / 4 * 4;
                }
                if (isCompressed) {
                    mipWidth = (mipWidth + 3) / 4 * gPixelFormatBits(inPixelFormat) / 2;
                } else {
                    mipWidth = mipWidth * gPixelFormatBits(inPixelFormat) / 8;
                }
                result += mipWidth * mipHeight;
                ++currentMip;
                --index;
            } while (index > 0);
        }
        return result;
    }

    private static int gGetNumPixelsInMipmaps(EPixelFormat pixelFormat, int width, int height, int numMipMaps) {
        int blockSize = switch (pixelFormat) {
            case S3TC1, S3TC3, S3TC5 -> 4;
            default -> 1;
        };
        int result = 0;
        if (numMipMaps >= 0) {
            int currentMip = (numMipMaps + 1);
            do {
                result = result + height * width;
                width = Math.max(width >> 1, blockSize);
                height = Math.max(height >> 1, blockSize);
                --currentMip;
            } while (currentMip > 0);
        }
        return result;
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private static int gPixelFormatBits(EPixelFormat format) {
        return switch (format) {
            case INDEX_4 -> 4;
            case INDEX_8 -> 8;
            case ALPHA_4 -> 4;
            case ALPHA_8 -> 8;
            case GLOW_8 -> 8;
            case INTENSITY_8 -> 8;
            case RGBA_8888 -> 0x20;
            case RGBA_8888_REV -> 0x20;
            case RGBA_5551 -> 0x10;
            case RGBA_5551_REV -> 0x10;
            case RGBA_4444 -> 0x10;
            case RGBA_4444_REV -> 0x10;
            case RGB_888_32 -> 0x20;
            case RGB_888_32_REV -> 0x20;
            case RGB_888 -> 0x18;
            case RGB_888_REV -> 0x18;
            case RGB_565 -> 0x10;
            case RGB_565_REV -> 0x10;
            case RGB_555 -> 0x10;
            case RGB_555_REV -> 0x10;
            case S3TC1 -> 4;
            case S3TC3 -> 8;
            case S3TC5 -> 8;
            case RGBE_REV -> 0x20;
            case INDEX_2X2 -> 4;
            case INDEX_2 -> 2;
            case FLOAT_32 -> 0x20;
            case RGB_FLOAT_32 -> 0x60;
            case RGBA_FLOAT_32 -> 0x80;
            case FLOAT_16 -> 0x10;
            case RG_FLOAT_16 -> 0x20;
            case RGB_FLOAT_16 -> 0x30;
            case RGBA_FLOAT_16 -> 0x40;
            case DEPTH_24_STENCIL_8 -> 0x20;
            case DEPTH_16_STENCIL_0 -> 0x10;
            case INVALID -> throw new IllegalArgumentException();
        };
    }
}
