package com.shade.decima.game.hrzr.storage.api;

import com.shade.util.NotNull;

public interface Asset extends Comparable<Asset> {
    @NotNull
    AssetId id();

    @Override
    default int compareTo(Asset o) {
        return id().compareTo(o.id());
    }
}
