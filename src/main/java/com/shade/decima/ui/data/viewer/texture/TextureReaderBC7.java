package com.shade.decima.ui.data.viewer.texture;

import com.shade.decima.ui.data.viewer.texture.util.BitBuffer;
import com.shade.decima.ui.data.viewer.texture.util.RGB;
import com.shade.util.NotNull;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TextureReaderBC7 extends TextureReader {
    public static class Provider implements TextureReaderProvider {
        @NotNull
        @Override
        public TextureReader create(@NotNull String format) {
            return new TextureReaderBC7();
        }

        @Override
        public boolean supports(@NotNull String format) {
            return format.equals("BC7");
        }
    }

    private static final ModeInfo[] MODES = {
        new ModeInfo(3, 4, 0, 0, 4, 0, 1, 0, 3, 0),
        new ModeInfo(2, 6, 0, 0, 6, 0, 0, 1, 3, 0),
        new ModeInfo(3, 6, 0, 0, 5, 0, 0, 0, 2, 0),
        new ModeInfo(2, 6, 0, 0, 7, 0, 1, 0, 2, 0),
        new ModeInfo(1, 0, 2, 1, 5, 6, 0, 0, 2, 3),
        new ModeInfo(1, 0, 2, 0, 7, 8, 0, 0, 2, 2),
        new ModeInfo(1, 0, 0, 0, 7, 7, 1, 0, 4, 0),
        new ModeInfo(2, 6, 0, 0, 5, 5, 1, 0, 2, 0)
    };

    /**
     * Subset indices: Table.P2, 1 bit per index
     */
    private static final int[] SUBSET_INDICES_2 = {
        0xcccc, 0x8888, 0xeeee, 0xecc8, 0xc880, 0xfeec, 0xfec8, 0xec80, 0xc800, 0xffec,
        0xfe80, 0xe800, 0xffe8, 0xff00, 0xfff0, 0xf000, 0xf710, 0x008e, 0x7100, 0x08ce,
        0x008c, 0x7310, 0x3100, 0x8cce, 0x088c, 0x3110, 0x6666, 0x366c, 0x17e8, 0x0ff0,
        0x718e, 0x399c, 0xaaaa, 0xf0f0, 0x5a5a, 0x33cc, 0x3c3c, 0x55aa, 0x9696, 0xa55a,
        0x73ce, 0x13c8, 0x324c, 0x3bdc, 0x6996, 0xc33c, 0x9966, 0x0660, 0x0272, 0x04e4,
        0x4e40, 0x2720, 0xc936, 0x936c, 0x39c6, 0x639c, 0x9336, 0x9cc6, 0x817e, 0xe718,
        0xccf0, 0x0fcc, 0x7744, 0xee22
    };

    /**
     * Subset indices: Table.P3, 2 bitmap per index
     */
    private static final int[] SUBSET_INDICES_3 = {
        0xaa685050, 0x6a5a5040, 0x5a5a4200, 0x5450a0a8, 0xa5a50000, 0xa0a05050, 0x5555a0a0,
        0x5a5a5050, 0xaa550000, 0xaa555500, 0xaaaa5500, 0x90909090, 0x94949494, 0xa4a4a4a4,
        0xa9a59450, 0x2a0a4250, 0xa5945040, 0x0a425054, 0xa5a5a500, 0x55a0a0a0, 0xa8a85454,
        0x6a6a4040, 0xa4a45000, 0x1a1a0500, 0x0050a4a4, 0xaaa59090, 0x14696914, 0x69691400,
        0xa08585a0, 0xaa821414, 0x50a4a450, 0x6a5a0200, 0xa9a58000, 0x5090a0a8, 0xa8a09050,
        0x24242424, 0x00aa5500, 0x24924924, 0x24499224, 0x50a50a50, 0x500aa550, 0xaaaa4444,
        0x66660000, 0xa5a0a5a0, 0x50a050a0, 0x69286928, 0x44aaaa44, 0x66666600, 0xaa444444,
        0x54a854a8, 0x95809580, 0x96969600, 0xa85454a8, 0x80959580, 0xaa141414, 0x96960000,
        0xaaaa1414, 0xa05050a0, 0xa0a5a5a0, 0x96000000, 0x40804080, 0xa9a8a9a8, 0xaaaaaa44,
        0x2a4a5254
    };

    /**
     * Anchor indices: Table.A2
     */
    public static final int[] ANCHOR_INDICES_0 = {
        15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 2, 8, 2, 2, 8,
        8, 15, 2, 8, 2, 2, 8, 8, 2, 2, 15, 15, 6, 8, 2, 8, 15, 15, 2, 8, 2, 2,
        2, 15, 15, 6, 6, 2, 6, 8, 15, 15, 2, 2, 15, 15, 15, 15, 15, 2, 2, 15
    };

    /**
     * Anchor indices: Table.A3a
     */
    public static final int[] ANCHOR_INDICES_1 = {
        3, 3, 15, 15, 8, 3, 15, 15, 8, 8, 6, 6, 6, 5, 3, 3, 3, 3, 8, 15, 3, 3,
        6, 10, 5, 8, 8, 6, 8, 5, 15, 15, 8, 15, 3, 5, 6, 10, 8, 15, 15, 3, 15, 5,
        15, 15, 15, 15, 3, 15, 5, 5, 5, 8, 5, 10, 5, 10, 8, 13, 15, 12, 3, 3
    };

    /**
     * Anchor indices: Table.A3b
     */
    public static final int[] ANCHOR_INDICES_2 = {
        15, 8, 8, 3, 15, 15, 3, 8, 15, 15, 15, 15, 15,
        15, 15, 8, 15, 8, 15, 3, 15, 8, 15, 8, 3, 15,
        6, 10, 15, 15, 10, 8, 15, 3, 15, 10, 10, 8, 9,
        10, 6, 15, 8, 15, 3, 6, 6, 8, 15, 3, 15, 15,
        15, 15, 15, 15, 15, 15, 15, 15, 3, 15, 15, 8
    };

    public static final int[] WEIGHTS_2 = {0, 21, 43, 64};
    public static final int[] WEIGHTS_3 = {0, 9, 18, 27, 37, 46, 55, 64};
    public static final int[] WEIGHTS_4 = {0, 4, 9, 13, 17, 21, 26, 30, 34, 38, 43, 47, 51, 55, 60, 64};

    protected TextureReaderBC7() {
        super(BufferedImage.TYPE_INT_ARGB, 8, 4);
    }

    @Override
    protected void readBlock(@NotNull ByteBuffer buffer, @NotNull BufferedImage image, int x, int y) {
        final BitBuffer reader = new BitBuffer(buffer, 16);

        if (reader.get(0, 8) == 0) {
            return;
        }

        for (int mode = 0; mode <= 7; mode++) {
            if (reader.get(1) == 0) {
                continue;
            }

            final var endpoints = new RGB[6];
            Arrays.fill(endpoints, new RGB(0));

            final var info = MODES[mode];
            final var partition = reader.get(info.pb);
            final var rotation = reader.get(info.rb);
            final var indexSel = reader.get(info.isb) > 0;
            final var subsets = info.ns << 1;
            final var colorBits = info.cb + (info.epb > 0 ? 1 : 0) + (info.spb > 0 ? 1 : 0);
            final var alphaBits = info.ab + (info.epb > 0 && info.ab > 0 ? 1 : 0) + (info.spb > 0 && info.ab > 0 ? 1 : 0);
            final var colorWeight = getWeights(info.ib);
            final var alphaWeight = getWeights(info.ab > 0 && info.ib2 > 0 ? info.ib2 : info.ib);

            for (int i = 0; i < subsets; i++) {
                endpoints[i] = endpoints[i].r(reader.get(info.cb));
            }

            for (int i = 0; i < subsets; i++) {
                endpoints[i] = endpoints[i].g(reader.get(info.cb));
            }

            for (int i = 0; i < subsets; i++) {
                endpoints[i] = endpoints[i].b(reader.get(info.cb));
            }

            for (int i = 0; i < subsets; i++) {
                endpoints[i] = endpoints[i].a(info.ab > 0 ? reader.get(info.ab) : 255);
            }

            if (info.epb > 0) {
                for (int i = 0; i < subsets; i++) {
                    final var val = reader.get(1);
                    endpoints[i] = endpoints[i].map(p -> p << 1 | val, alphaBits > 0);
                }
            }

            if (info.spb > 0) {
                for (int i = 0; i < subsets; i += 2) {
                    final var val = reader.get(1);
                    for (int j = 0; j < 2; j++) {
                        endpoints[i + j] = endpoints[i + j].map(p -> p << 1 | val, alphaBits > 0);
                    }
                }
            }

            for (int i = 0; i < subsets; i++) {
                endpoints[i] = endpoints[i].map((pi, p) -> unquantize(p, pi < 3 ? colorBits : alphaBits), alphaBits > 0);
            }

            int cibit = reader.position();
            int aibit = cibit + 16 * info.ib - info.ns;

            for (int i = 0; i < 16; i++) {
                final int subset = getSubset(info.ns, partition, i) << 1;
                final int indexBits;

                if (i == 0) {
                    indexBits = info.ib - 1;
                } else if (info.ns == 2 && ANCHOR_INDICES_0[partition] == i) {
                    indexBits = info.ib - 1;
                } else if (info.ns == 3 && ANCHOR_INDICES_1[partition] == i) {
                    indexBits = info.ib - 1;
                } else if (info.ns == 3 && ANCHOR_INDICES_2[partition] == i) {
                    indexBits = info.ib - 1;
                } else {
                    indexBits = info.ib;
                }

                final var index0 = reader.get(cibit, indexBits);
                cibit += indexBits;

                final RGB color;

                if (info.ib2 > 0) {
                    final var indexBits2 = info.ib2 - (i == 0 ? 1 : 0);
                    final var index1 = reader.get(aibit, indexBits2);
                    aibit += indexBits2;

                    if (indexSel) {
                        color = lerp(endpoints[subset], endpoints[subset + 1], alphaWeight[index1], colorWeight[index0]);
                    } else {
                        color = lerp(endpoints[subset], endpoints[subset + 1], colorWeight[index0], alphaWeight[index1]);
                    }
                } else {
                    color = lerp(endpoints[subset], endpoints[subset + 1], colorWeight[index0], colorWeight[index0]);
                }

                final RGB rotated = switch (rotation) {
                    case 1 -> new RGB(color.a(), color.g(), color.b(), color.r());
                    case 2 -> new RGB(color.r(), color.a(), color.b(), color.g());
                    case 3 -> new RGB(color.r(), color.g(), color.a(), color.b());
                    default -> color;
                };

                image.setRGB(x + i % 4, y + i / 4, rotated.argb());
            }

            break;
        }
    }

    @NotNull
    public static int[] getWeights(int n) {
        return switch (n) {
            case 2 -> WEIGHTS_2;
            case 3 -> WEIGHTS_3;
            default -> WEIGHTS_4;
        };
    }

    public static int getSubset(int ns, int partition, int n) {
        return switch (ns) {
            case 2 -> SUBSET_INDICES_2[partition] >>> n & 1;
            case 3 -> SUBSET_INDICES_3[partition] >>> n * 2 & 3;
            default -> 0;
        };
    }

    private static int unquantize(int v, int bits) {
        int t = v << 8 - bits;
        return t | t >>> bits;
    }

    private static int lerp(int e0, int e1, int weight) {
        return (64 - weight) * e0 + weight * e1 + 32 >>> 6;
    }

    @NotNull
    private static RGB lerp(@NotNull RGB e0, @NotNull RGB e1, int wc, int wa) {
        return new RGB(
            lerp(e0.r(), e1.r(), wc),
            lerp(e0.g(), e1.g(), wc),
            lerp(e0.b(), e1.b(), wc),
            lerp(e0.a(), e1.a(), wa)
        );
    }

    private static record ModeInfo(
        /* Number of subsets */
        int ns,
        /* Partition selection bitmap */
        int pb,
        /* Rotation bitmap */
        int rb,
        /* Index selection bit */
        int isb,
        /* Colors bitmap */
        int cb,
        /* Alpha bitmap */
        int ab,
        /* Endpoint P-bitmap (all channels) */
        int epb,
        /* Shared P-bitmap */
        int spb,
        /* Index bitmap */
        int ib,
        /* Secondary index bitmap */
        int ib2
    ) {}
}
