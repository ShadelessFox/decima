package com.shade.decima.ui.data.viewer.wwise.data;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public enum AkBankSourceType {
    DATA,
    PREFETCH_STREAMING,
    STREAMING;

    @NotNull
    public static AkBankSourceType read(@NotNull ByteBuffer buffer) {
        return values()[buffer.get()];
    }
}
