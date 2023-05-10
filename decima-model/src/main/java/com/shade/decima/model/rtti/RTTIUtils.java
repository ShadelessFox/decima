package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

public class RTTIUtils {
    private RTTIUtils() {
        // prevents instantiation
    }

    @NotNull
    public static String uuidToString(@NotNull RTTIObject o) {
        if (!o.type().isInstanceOf("GGUUID")) {
            throw new IllegalArgumentException("Object is not an instance of GGUUID");
        }

        return "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".formatted(
            o.i8("Data3"), o.i8("Data2"), o.i8("Data1"), o.i8("Data0"),
            o.i8("Data5"), o.i8("Data4"),
            o.i8("Data7"), o.i8("Data6"),
            o.i8("Data8"), o.i8("Data9"),
            o.i8("Data10"), o.i8("Data11"), o.i8("Data12"), o.i8("Data13"), o.i8("Data14"), o.i8("Data15")
        );
    }
}
