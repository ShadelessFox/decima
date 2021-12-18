package com.shade.decima.util;

public final class CipherUtils {
    public static final int CIPHER_SEED = 0x2A;
    public static final int[] PLAIN_CIPHER_KEY = {0x0FA3A9443, 0x0F41CAB62, 0x0F376811C, 0x0D2A89E3E};
    public static final int[] CHUNK_CIPHER_KEY = {0x06C084A37, 0x07E159D95, 0x03D5AF7E8, 0x018AA7D3F};

    private CipherUtils() {
    }
}
