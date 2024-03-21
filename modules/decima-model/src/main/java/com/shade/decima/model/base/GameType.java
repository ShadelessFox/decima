package com.shade.decima.model.base;

import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.file.Path;

public enum GameType {
    DS("Death Stranding"),
    DSDC("Death Stranding (Director's Cut)"),
    HZD("Horizon Zero Dawn"),
    HFW("Horizon Forbidden West");

    private final String name;

    GameType(@NotNull String name) {
        this.name = name;
    }

    // Note regarding "known" values:
    // We do know that these files exist, and we may use this information wisely.
    // I'd like to embed these files in resources and choose based on the selected
    // game type, but it might be useful to be able to specify custom values for these

    @NotNull
    public Path getKnownRttiTypesPath() {
        return switch (this) {
            case DS -> Path.of("data/ds_types.json.gz");
            case DSDC -> Path.of("data/dsdc_types.json.gz");
            case HZD -> Path.of("data/hzd_types.json.gz");
            case HFW -> throw new NotImplementedException();
        };
    }

    @NotNull
    public Path getKnownFileListingsPath() {
        return switch (this) {
            case DS -> Path.of("data/ds_paths.txt.gz");
            case DSDC -> Path.of("data/dsdc_paths.txt.gz");
            case HZD -> Path.of("data/hzd_paths.txt.gz");
            case HFW -> throw new NotImplementedException();
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
