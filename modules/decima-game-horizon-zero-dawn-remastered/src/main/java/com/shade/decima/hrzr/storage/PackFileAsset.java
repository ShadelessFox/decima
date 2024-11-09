package com.shade.decima.hrzr.storage;

import com.shade.decima.storage.Asset;
import com.shade.util.NotNull;

public record PackFileAsset(@NotNull PackFileAssetId id, long offset, int length) implements Asset {
}
