package com.shade.decima.ui.data.viewer.audio.data.mpeg;

/**
 * Represents an MPEG audio frame header.
 *
 * @see <a href="https://www.datavoyage.com/mpgscript/mpeghdr.htm">MPEG Audio Layer I/II/III frame header</a>
 */
public record MpegFrameHeader(int raw) {
    public static final int MPEG_1 = 0b1;
    public static final int MPEG_2 = 0b0;

    public static final int LAYER_1 = 0b11;
    public static final int LAYER_2 = 0b10;
    public static final int LAYER_3 = 0b01;

    private static final int[] BITRATE_V1_L1 = {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448};
    private static final int[] BITRATE_V1_L2 = {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384};
    private static final int[] BITRATE_V1_L3 = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};
    private static final int[] BITRATE_V2_L1 = {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256};
    private static final int[] BITRATE_V2_L2 = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160};

    public MpegFrameHeader {
        if (raw >>> 20 != 0xfff) {
            throw new IllegalArgumentException("Invalid frame sync");
        }
    }

    public int samplesPerFrame() {
        return switch (layerId()) {
            case LAYER_1 -> 384;
            case LAYER_2 -> 1152;
            case LAYER_3 -> mpegId() == MPEG_1 ? 1152 : 576;
            default -> throw new IllegalStateException();
        };
    }

    public int samplingRate() {
        int sampleRate = switch (frequencyIndex()) {
            case 0 -> 44100;
            case 1 -> 48000;
            case 2 -> 32000;
            default -> throw new IllegalStateException();
        };
        return mpegId() == MPEG_1 ? sampleRate : sampleRate >> 1;
    }

    public int bitRate() {
        if (mpegId() == MPEG_1) {
            if (layerId() == LAYER_1) {
                return BITRATE_V1_L1[bitRateIndex()];
            } else if (layerId() == LAYER_2) {
                return BITRATE_V1_L2[bitRateIndex()];
            } else {
                return BITRATE_V1_L3[bitRateIndex()];
            }
        } else {
            if (layerId() == LAYER_1) {
                return BITRATE_V2_L1[bitRateIndex()];
            } else {
                return BITRATE_V2_L2[bitRateIndex()];
            }
        }
    }

    public int paddingSize() {
        if (layerId() == LAYER_1) {
            return 4;
        } else {
            return 1;
        }
    }

    public int frameSize() {
        int frameSize = bitRate() * 144000 / samplingRate();
        if (channelMode() == 3) {
            frameSize >>= 1;
        }
        if (protectionBit() == 0) {
            frameSize -= 2;
        }
        if (paddingBit() == 1) {
            frameSize += paddingSize();
        }
        return frameSize;
    }

    public int frameSync() {
        return raw >>> 20;
    }

    public int mpegId() {
        return raw >>> 19 & 1;
    }

    public int layerId() {
        return raw >>> 17 & 3;
    }

    public int protectionBit() {
        return raw >>> 16 & 1;
    }

    public int bitRateIndex() {
        return raw >>> 12 & 15;
    }

    public int frequencyIndex() {
        return raw >>> 10 & 3;
    }

    public int paddingBit() {
        return raw >>> 9 & 1;
    }

    public int privateBit() {
        return raw >>> 8 & 1;
    }

    public int channelMode() {
        return raw >>> 6 & 3;
    }

    public int modeExtension() {
        return raw >>> 4 & 3;
    }

    public int copyright() {
        return raw >>> 3 & 1;
    }

    public int original() {
        return raw >>> 2 & 1;
    }

    public int emphasis() {
        return raw & 3;
    }

    @Override
    public String toString() {
        return String.format(
            "MpegFrameHeader[frameSync=%x, mpegId=%d, layerId=%d, protectionBit=%d, bitrateIndex=%d, frequencyIndex=%d, paddingBit=%d, privateBit=%d, channelMode=%d, modeExtension=%d, copyright=%d, original=%d, emphasis=%d]",
            frameSync(), mpegId(), layerId(), protectionBit(), bitRateIndex(), frequencyIndex(), paddingBit(), privateBit(), channelMode(), modeExtension(), copyright(), original(), emphasis()
        );
    }
}
