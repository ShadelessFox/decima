package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.hrzr.storage.api.AssetId;
import com.shade.util.NotNull;
import com.shade.util.hash.Hashing;

import java.util.Locale;

public record PackFileAssetId(long hash) implements AssetId {
    @NotNull
    public static PackFileAssetId ofHash(long hash) {
        return new PackFileAssetId(hash);
    }

    @NotNull
    public static PackFileAssetId ofPath(@NotNull String path) {
        var norm = path.toLowerCase(Locale.ROOT) + '\0';
        var hash = Hashing.decimaMurmur3().hashString(norm).asLong();
        return new PackFileAssetId(hash);
    }

    @Override
    public int compareTo(@NotNull AssetId o) {
        return Long.compareUnsigned(hash, ((PackFileAssetId) o).hash);
    }
}
