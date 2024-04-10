package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RTTIUtils {
    private RTTIUtils() {
        // prevents instantiation
    }

    @NotNull
    public static String uuidToString(@NotNull RTTIObject o) {
        return "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".formatted(
            o.i8("Data0"),
            o.i8("Data1"),
            o.i8("Data2"),
            o.i8("Data3"),
            o.i8("Data4"),
            o.i8("Data5"),
            o.i8("Data6"),
            o.i8("Data7"),
            o.i8("Data8"),
            o.i8("Data9"),
            o.i8("Data10"),
            o.i8("Data11"),
            o.i8("Data12"),
            o.i8("Data13"),
            o.i8("Data14"),
            o.i8("Data15")
        );
    }

    @NotNull
    public static RTTIObject uuidFromString(@NotNull RTTIClass type, @NotNull String text) {
        final UUID uuid;

        if (text.startsWith("{") && text.endsWith("}")) {
            uuid = UUID.fromString(text.substring(1, text.length() - 1));
        } else {
            uuid = UUID.fromString(text);
        }

        final long msb = uuid.getMostSignificantBits();
        final long lsb = uuid.getLeastSignificantBits();
        final RTTIObject object = type.create();

        object.set("Data0", (byte) (msb >>> 56));
        object.set("Data1", (byte) (msb >>> 48));
        object.set("Data2", (byte) (msb >>> 40));
        object.set("Data3", (byte) (msb >>> 32));
        object.set("Data4", (byte) (msb >>> 24));
        object.set("Data5", (byte) (msb >>> 16));
        object.set("Data6", (byte) (msb >>> 8));
        object.set("Data7", (byte) (msb));
        object.set("Data8", (byte) (lsb >>> 56));
        object.set("Data9", (byte) (lsb >>> 48));
        object.set("Data10", (byte) (lsb >>> 40));
        object.set("Data11", (byte) (lsb >>> 32));
        object.set("Data12", (byte) (lsb >>> 24));
        object.set("Data13", (byte) (lsb >>> 16));
        object.set("Data14", (byte) (lsb >>> 8));
        object.set("Data15", (byte) (lsb));

        return object;
    }

    @NotNull
    public static RTTIObject randomUUID(@NotNull RTTIClass type) {
        final byte[] bytes = new byte[16];
        ThreadLocalRandom.current().nextBytes(bytes);

        final RTTIObject object = type.create();
        object.set("Data0", bytes[0]);
        object.set("Data1", bytes[1]);
        object.set("Data2", bytes[2]);
        object.set("Data3", bytes[3]);
        object.set("Data4", bytes[4]);
        object.set("Data5", bytes[5]);
        object.set("Data6", bytes[6]);
        object.set("Data7", bytes[7]);
        object.set("Data8", bytes[8]);
        object.set("Data9", bytes[9]);
        object.set("Data10", bytes[10]);
        object.set("Data11", bytes[11]);
        object.set("Data12", bytes[12]);
        object.set("Data13", bytes[13]);
        object.set("Data14", bytes[14]);
        object.set("Data15", bytes[15]);

        return object;
    }
}
