package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;

public record RTTIReference(@NotNull Type type, @Nullable RTTIObject uuid, @Nullable String path) {
    public record FollowResult(@NotNull CoreBinary binary, @NotNull RTTIObject object) {}

    @NotNull
    public FollowResult follow(@NotNull CoreBinary current, @NotNull Packfile packfile, @NotNull RTTITypeRegistry registry) throws IOException {
        if (uuid == null) {
            throw new IllegalArgumentException("Invalid reference");
        }

        final CoreBinary binary;

        if (path != null) {
            binary = CoreBinary.from(packfile.extract(PackfileBase.getNormalizedPath(path)), registry);
        } else {
            binary = current;
        }

        final RTTIObject object = binary.find(uuid);

        if (object == null) {
            throw new IllegalArgumentException("Can't find referenced file");
        }

        return new FollowResult(binary, object);
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
