package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.Asset;
import com.shade.util.NotNull;

public record PackFileAsset(@NotNull PackFileAssetId id, long offset, int length) implements Asset {
}
