package com.shade.util.hash;

import com.shade.util.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface HashFunction {
    int bits();

    @NotNull
    HashCode hashBytes(@NotNull byte[] input, int off, int len);

    @NotNull
    default HashCode hashBytes(@NotNull byte[] input) {
        return hashBytes(input, 0, input.length);
    }

    @NotNull
    default HashCode hashString(@NotNull CharSequence input, @NotNull Charset charset) {
        return hashBytes(input.toString().getBytes(charset));
    }

    @NotNull
    default HashCode hashString(@NotNull CharSequence input) {
        return hashString(input, StandardCharsets.UTF_8);
    }
}
