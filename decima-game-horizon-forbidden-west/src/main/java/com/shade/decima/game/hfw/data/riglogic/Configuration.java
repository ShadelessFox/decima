package com.shade.decima.game.hfw.data.riglogic;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;

public record Configuration(
    @NotNull CalculationType calculationType
) {
    @NotNull
    public static Configuration read(@NotNull BinaryReader reader) throws IOException {
        return new Configuration(
            CalculationType.values()[reader.readInt()]
        );
    }

}
