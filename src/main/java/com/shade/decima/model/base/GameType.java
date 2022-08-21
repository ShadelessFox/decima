package com.shade.decima.model.base;

import com.shade.util.NotNull;

public enum GameType {
    DS("Death Stranding"),
    HZD("Horizon Zero Dawn");

    private final String name;

    GameType(@NotNull String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
