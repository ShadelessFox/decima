package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public record RTTIReference(@NotNull Type type, @Nullable RTTIObject uuid, @Nullable String path) {
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
