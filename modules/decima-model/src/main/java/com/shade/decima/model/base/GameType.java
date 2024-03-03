package com.shade.decima.model.base;

import com.shade.util.NotNull;

public enum GameType {
    DS("Death Stranding"),
    DSDC("Death Stranding (Director's Cut)"),
    HZD("Horizon Zero Dawn"),
    HFW("Horizon Forbidden West");

    private final String name;

    GameType(@NotNull String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
