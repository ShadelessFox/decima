package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.AssetId;
import com.shade.util.NotNull;
import com.shade.util.hash.HashFunction;

import java.util.Locale;

public record PackFileAssetId(long hash) implements AssetId {
    @NotNull
    public static PackFileAssetId ofHash(long hash) {
        return new PackFileAssetId(hash);
    }

    @NotNull
    public static PackFileAssetId ofPath(@NotNull String path) {
        var norm = path.toLowerCase(Locale.ROOT) + '\0';
        var hash = HashFunction.murmur3().hash(norm).asLong();
        return new PackFileAssetId(hash);
    }
}
