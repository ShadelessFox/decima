package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.Asset;

public record PackFileAsset(PackFileAssetId id, long offset, int length) implements Asset {
}
