package com.shade.decima.hrzr.storage;

import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.decima.storage.AssetId;
import com.shade.util.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public record PackFileAssetId(long hash) implements AssetId {
    @NotNull
    public static AssetId ofHash(long hash) {
        return new PackFileAssetId(hash);
    }

    @NotNull
    public static PackFileAssetId ofPath(@NotNull String path) {
        var normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        var data = normalized.getBytes(StandardCharsets.UTF_8);
        var cstr = Arrays.copyOf(data, data.length + 1);
        return new PackFileAssetId(MurmurHash3.mmh3(cstr)[0]);
    }

    @Override
    public int compareTo(@NotNull AssetId o) {
        return Long.compareUnsigned(hash, ((PackFileAssetId) o).hash);
    }
}
