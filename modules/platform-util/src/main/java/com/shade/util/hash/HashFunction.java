package com.shade.util.hash;

import com.shade.util.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract sealed class HashFunction
    permits Crc32Function, Fnv1aFunction, Murmur3Function {

    @NotNull
    public static HashFunction fnv1a() {
        return Fnv1aFunction.FNV1A;
    }

    @NotNull
    public static HashFunction crc32c() {
        return Crc32Function.CRC32C;
    }

    @NotNull
    public static HashFunction murmur3() {
        return Murmur3Function.MURMUR3;
    }

    @NotNull
    public abstract HashCode hash(@NotNull byte[] input, int off, int len);

    @NotNull
    public HashCode hash(@NotNull byte[] input) {
        return hash(input, 0, input.length);
    }

    @NotNull
    public HashCode hash(@NotNull CharSequence input, @NotNull Charset charset) {
        return hash(input.toString().getBytes(charset));
    }

    @NotNull
    public HashCode hash(@NotNull CharSequence input) {
        return hash(input, StandardCharsets.UTF_8);
    }
}
