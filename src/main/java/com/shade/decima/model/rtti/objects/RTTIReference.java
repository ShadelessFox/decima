package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public class RTTIReference {
    private final Type type;
    private final RTTIObject uuid;
    private final String path;

    public RTTIReference(@NotNull Type type, @Nullable RTTIObject uuid, @Nullable String path) {
        this.type = type;
        this.uuid = uuid;
        this.path = path;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Nullable
    public RTTIObject getUuid() {
        return uuid;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RTTIReference[type=").append(type);
        if (type.hasUuid) {
            sb.append(", uuid=").append(uuid);
        }
        if (type.hasPath) {
            sb.append(", path=").append(path);
        }
        return sb.append(']').toString();
    }

    public enum Type {
        NONE(0, false, false),
        INTERNAL_LINK(1, true, false),
        EXTERNAL_LINK(2, true, true),
        STREAMING_REFERENCE(3, true, true),
        UUID_REFERENCE(5, true, false);

        private final byte value;
        private final boolean hasUuid;
        private final boolean hasPath;

        Type(int value, boolean hasUuid, boolean hasPath) {
            this.value = (byte) value;
            this.hasUuid = hasUuid;
            this.hasPath = hasPath;
        }

        @NotNull
        public static Type valueOf(int value) {
            for (Type type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown reference type " + value);
        }

        public byte getValue() {
            return value;
        }

        public boolean hasUuid() {
            return hasUuid;
        }

        public boolean hasPath() {
            return hasPath;
        }
    }
}
