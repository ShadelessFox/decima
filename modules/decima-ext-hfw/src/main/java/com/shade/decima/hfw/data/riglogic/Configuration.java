package com.shade.decima.hfw.data.riglogic;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record Configuration(
    @NotNull CalculationType calculationType
) {
    @NotNull
    public static Configuration read(@NotNull ByteBuffer buffer) {
        return new Configuration(
            CalculationType.values()[buffer.getInt()]
        );
    }

}
