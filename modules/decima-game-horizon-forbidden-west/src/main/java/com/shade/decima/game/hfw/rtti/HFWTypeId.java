package com.shade.decima.game.hfw.rtti;

import com.shade.decima.rtti.factory.TypeId;
import com.shade.util.NotNull;

public record HFWTypeId(long hash) implements TypeId {
    @NotNull
    public static TypeId of(long hash) {
        return new HFWTypeId(hash);
    }
}
