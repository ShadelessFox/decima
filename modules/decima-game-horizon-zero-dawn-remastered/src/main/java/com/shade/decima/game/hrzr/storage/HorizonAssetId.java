package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.AssetId;
import com.shade.util.NotNull;
import com.shade.util.hash.HashFunction;

import java.util.Locale;
import java.util.UUID;

public record HorizonAssetId(long fileHash, @NotNull UUID objectUuid) implements AssetId {
    public static HorizonAssetId ofPath(@NotNull String path, @NotNull String uuid) {
        var norm = path.toLowerCase(Locale.ROOT) + '\0';
        var hash = HashFunction.murmur3().hash(norm).asLong();
        return new HorizonAssetId(hash, UUID.fromString(uuid));
    }
}
