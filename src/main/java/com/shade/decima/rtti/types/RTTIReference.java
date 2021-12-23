package com.shade.decima.rtti.types;

import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.util.UUID;

public class RTTIReference {
    private final Type type;
    private final UUID uuid;
    private final String path;

    public RTTIReference(@NotNull Type type, @Nullable UUID uuid, @Nullable String path) {
        this.type = type;
        this.uuid = uuid;
        this.path = path;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Nullable
    public UUID getUuid() {
        return uuid;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public enum Type {
        NONE,
        INTERNAL_LINK,
        EXTERNAL_LINK,
        STREAMING_REFERENCE,
        UNKNOWN,
        UUID_REFERENCE
    }
}
