package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.decima.ui.data.viewer.texture.util.BitBuffer;
import com.shade.decima.ui.data.viewer.texture.util.RGB;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

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
    }

    private static final ModeInfo[] MODES = {
        // @formatter:off
        //           ib ns tr     pb epb rb  gb  bb  bits
        new ModeInfo(3, 2, true,  5, 10,  5,  5,  5, new int[]{116, 132, 180, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 48, 49, 50, 51, 52, 164, 112, 113, 114, 115, 64, 65, 66, 67, 68, 176, 160, 161, 162, 163, 80, 81, 82, 83, 84, 177, 128, 129, 130, 131, 96, 97, 98, 99, 100, 178, 144, 145, 146, 147, 148, 179}),
        new ModeInfo(3, 2, true,  5,  7,  6,  6,  6, new int[]{117, 164, 165, 0, 1, 2, 3, 4, 5, 6, 176, 177, 132, 16, 17, 18, 19, 20, 21, 22, 133, 178, 116, 32, 33, 34, 35, 36, 37, 38, 179, 181, 180, 48, 49, 50, 51, 52, 53, 112, 113, 114, 115, 64, 65, 66, 67, 68, 69, 160, 161, 162, 163, 80, 81, 82, 83, 84, 85, 128, 129, 130, 131, 96, 97, 98, 99, 100, 101, 144, 145, 146, 147, 148, 149}),
        new ModeInfo(3, 2, true,  5, 11,  5,  4,  4, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 48, 49, 50, 51, 52, 10, 112, 113, 114, 115, 64, 65, 66, 67, 26, 176, 160, 161, 162, 163, 80, 81, 82, 83, 42, 177, 128, 129, 130, 131, 96, 97, 98, 99, 100, 178, 144, 145, 146, 147, 148, 179}),
        new ModeInfo(3, 2, true,  5, 11,  4,  5,  4, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 48, 49, 50, 51, 10, 164, 112, 113, 114, 115, 64, 65, 66, 67, 68, 26, 160, 161, 162, 163, 80, 81, 82, 83, 42, 177, 128, 129, 130, 131, 96, 97, 98, 99, 176, 178, 144, 145, 146, 147, 116, 179}),
        new ModeInfo(3, 2, true,  5, 11,  4,  4,  5, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 48, 49, 50, 51, 10, 132, 112, 113, 114, 115, 64, 65, 66, 67, 26, 176, 160, 161, 162, 163, 80, 81, 82, 83, 84, 42, 128, 129, 130, 131, 96, 97, 98, 99, 177, 178, 144, 145, 146, 147, 180, 179}),
        new ModeInfo(3, 2, true,  5,  9,  5,  5,  5, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 132, 16, 17, 18, 19, 20, 21, 22, 23, 24, 116, 32, 33, 34, 35, 36, 37, 38, 39, 40, 180, 48, 49, 50, 51, 52, 164, 112, 113, 114, 115, 64, 65, 66, 67, 68, 176, 160, 161, 162, 163, 80, 81, 82, 83, 84, 177, 128, 129, 130, 131, 96, 97, 98, 99, 100, 178, 144, 145, 146, 147, 148, 179}),
        new ModeInfo(3, 2, true,  5,  8,  6,  5,  5, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 164, 132, 16, 17, 18, 19, 20, 21, 22, 23, 178, 116, 32, 33, 34, 35, 36, 37, 38, 39, 179, 180, 48, 49, 50, 51, 52, 53, 112, 113, 114, 115, 64, 65, 66, 67, 68, 176, 160, 161, 162, 163, 80, 81, 82, 83, 84, 177, 128, 129, 130, 131, 96, 97, 98, 99, 100, 101, 144, 145, 146, 147, 148, 149}),
        new ModeInfo(3, 2, true,  5,  8,  5,  6,  5, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 176, 132, 16, 17, 18, 19, 20, 21, 22, 23, 117, 116, 32, 33, 34, 35, 36, 37, 38, 39, 165, 180, 48, 49, 50, 51, 52, 164, 112, 113, 114, 115, 64, 65, 66, 67, 68, 69, 160, 161, 162, 163, 80, 81, 82, 83, 84, 177, 128, 129, 130, 131, 96, 97, 98, 99, 100, 178, 144, 145, 146, 147, 148, 179}),
        new ModeInfo(3, 2, true,  5,  8,  5,  5,  6, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 177, 132, 16, 17, 18, 19, 20, 21, 22, 23, 133, 116, 32, 33, 34, 35, 36, 37, 38, 39, 181, 180, 48, 49, 50, 51, 52, 164, 112, 113, 114, 115, 64, 65, 66, 67, 68, 176, 160, 161, 162, 163, 80, 81, 82, 83, 84, 85, 128, 129, 130, 131, 96, 97, 98, 99, 100, 178, 144, 145, 146, 147, 148, 179}),
        new ModeInfo(3, 2, false, 5,  6,  6,  6,  6, new int[]{0, 1, 2, 3, 4, 5, 164, 176, 177, 132, 16, 17, 18, 19, 20, 21, 117, 133, 178, 116, 32, 33, 34, 35, 36, 37, 165, 179, 181, 180, 48, 49, 50, 51, 52, 53, 112, 113, 114, 115, 64, 65, 66, 67, 68, 69, 160, 161, 162, 163, 80, 81, 82, 83, 84, 85, 128, 129, 130, 131, 96, 97, 98, 99, 100, 101, 144, 145, 146, 147, 148, 149}),
        new ModeInfo(4, 1, false, 0, 10, 10, 10, 10, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89}),
        new ModeInfo(4, 1, true,  0, 11,  9,  9,  9, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 48, 49, 50, 51, 52, 53, 54, 55, 56, 10, 64, 65, 66, 67, 68, 69, 70, 71, 72, 26, 80, 81, 82, 83, 84, 85, 86, 87, 88, 42}),
        new ModeInfo(4, 1, true,  0, 12,  8,  8,  8, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 48, 49, 50, 51, 52, 53, 54, 55, 11, 10, 64, 65, 66, 67, 68, 69, 70, 71, 27, 26, 80, 81, 82, 83, 84, 85, 86, 87, 43, 42}),
        new ModeInfo(4, 1, true,  0, 16,  4,  4,  4, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 48, 49, 50, 51, 15, 14, 13, 12, 11, 10, 64, 65, 66, 67, 31, 30, 29, 28, 27, 26, 80, 81, 82, 83, 47, 46, 45, 44, 43, 42})
        // @formatter:on
    };

    private final boolean signed;

    public ImageReaderBC6(boolean signed) {
        super(BufferedImage.TYPE_INT_RGB, 8, 4);
        this.signed = signed;
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final var bits = new BitBuffer(buffer, 16);
        final var info = getModeInfo(bits);
        final var endpoints = new short[12];

        for (int mask : info.bits) {
            final var ei = (mask >>> 4) & 15;
            final var bi = mask & 15;
            endpoints[ei] |= bits.get(1) << bi;
        }

        final var partition = bits.get(info.partitionBits);

        if (signed) {
            endpoints[0] = IOUtils.signExtend(endpoints[0], info.endpointsBits);
            endpoints[1] = IOUtils.signExtend(endpoints[1], info.endpointsBits);
            endpoints[2] = IOUtils.signExtend(endpoints[2], info.endpointsBits);
        }

        if (signed || info.transformedEndpoints) {
            for (int i = 3; i < 12; i += 3) {
                endpoints[i + 0] = IOUtils.signExtend(endpoints[i + 0], info.redBits);
                endpoints[i + 1] = IOUtils.signExtend(endpoints[i + 1], info.greenBits);
                endpoints[i + 2] = IOUtils.signExtend(endpoints[i + 2], info.blueBits);
            }
        }

        if (info.transformedEndpoints) {
            for (int i = 3; i < 12; i += 3) {
                final int mask = (1 << info.endpointsBits) - 1;
                endpoints[i + 0] = (short) (endpoints[i + 0] + endpoints[0] & mask);
                endpoints[i + 1] = (short) (endpoints[i + 1] + endpoints[1] & mask);
                endpoints[i + 2] = (short) (endpoints[i + 2] + endpoints[2] & mask);
            }
        }

        for (int i = 0; i < 12; i++) {
            endpoints[i] = unquantize(endpoints[i], info.endpointsBits, signed);
        }

        final int[] weights = ImageReaderBC7.getWeights(info.indexBits);

        for (int i = 0; i < 16; i++) {
            final int ib2;

            if (i == 0 || info.subsets == 2 && ImageReaderBC7.ANCHOR_INDICES_0[partition] == i) {
                ib2 = info.indexBits - 1;
            } else {
                ib2 = info.indexBits;
            }

            final int subset = ImageReaderBC7.getSubset(info.subsets, partition, i) * 6;
            final int index = bits.get(ib2);

            var col = new RGB(
                lerp(endpoints[subset + 0], endpoints[subset + 3], weights[index], signed),
                lerp(endpoints[subset + 1], endpoints[subset + 4], weights[index], signed),
                lerp(endpoints[subset + 2], endpoints[subset + 5], weights[index], signed)
            );

            image.setRGB(x + i % 4, y + i / 4, col.argb());
        }
    }

    @NotNull
    private ModeInfo getModeInfo(@NotNull BitBuffer bits) {
        return switch (bits.get(2)) {
            case 0 -> MODES[0];
            case 1 -> MODES[1];
            case 2 -> MODES[bits.get(3) + 2];
            default -> MODES[bits.get(3) + 10];
        };
    }

    private static int lerp(short e0, short e1, int weight, boolean signed) {
        final var interpolated = (64 - weight) * (e0 & 0xffff) + weight * (e1 & 0xffff) + 32 >>> 6;
        final var finalized = finalize(interpolated, signed);
        final var corrected = gamma(finalized, 1.0f, 2.2f);
        final var limited = Math.max(Math.min(corrected, 1), 0);
        return (int) (limited * 255);
    }

    private static float gamma(float value, float exposure, float gamma) {
        return (float) Math.pow(1.0f - Math.exp(-value * exposure), 1.0f / gamma);
    }

    private static float finalize(int value, boolean signed) {
        if (signed) {
            if (value < 0) {
                value = -value * 31 / 32;
                return IOUtils.halfToFloat((short) (0x8000 | value));
            } else {
                return IOUtils.halfToFloat((short) ((value * 31) / 32));
            }
        } else {
            return IOUtils.halfToFloat((short) (value * 31 / 64));
        }
    }

    private static short unquantize(short x, int ebp, boolean signed) {
        int unq;

        if (signed) {
            if (ebp >= 16) {
                unq = x;
            } else {
                boolean sign = false;

                if (x < 0) {
                    sign = true;
                    x = (short) -x;
                }

                if (x == 0) {
                    unq = 0;
                } else if (x >= (1 << ebp - 1) - 1) {
                    unq = 0x7FFF;
                } else {
                    unq = (x << 15) + 0x4000 >>> ebp - 1;
                }

                if (sign) {
                    unq = -unq;
                }
            }
        } else {
            if (ebp >= 15) {
                unq = x;
            } else if (x == 0) {
                unq = 0;
            } else if (x == (1 << ebp) - 1) {
                unq = 0xFFFF;
            } else {
                unq = (x << 15) + 0x4000 >>> ebp - 1;
            }
        }

        return (short) (unq & 0xffff);
    }

    private record ModeInfo(
        int indexBits,
        int subsets,
        boolean transformedEndpoints,
        int partitionBits,
        int endpointsBits,
        int redBits,
        int greenBits,
        int blueBits,
        int[] bits
    ) {}
}
