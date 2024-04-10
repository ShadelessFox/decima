package com.shade.decima.model.rtti;

import com.google.gson.stream.JsonWriter;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.RTTITypeNumber;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.UUID;

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
    public static RTTIObject uuidFromString(@NotNull RTTITypeClass type, @NotNull String text) {
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

    public static void serialize(@NotNull Object object, @NotNull RTTIType<?> type, @NotNull JsonWriter writer) throws IOException {
        if (object instanceof RTTIObject obj) {
            if (obj.type().isInstanceOf("GGUUID")) {
                writer.value(uuidToString(obj));
                return;
            }

            writer.beginObject();

            writer.name("$type");
            writer.value(obj.type().getFullTypeName());

            for (RTTIClass.Field<?> field : obj.type().getFields()) {
                final Object value = field.get((RTTIObject) object);

                if (value != null) {
                    writer.name(field.getName());
                    serialize(value, field.getType(), writer);
                }
            }

            writer.endObject();
        } else if (object instanceof RTTIReference) {
            writer.value(object.toString());
        } else if (type instanceof RTTITypeArray<?> array) {
            writer.beginArray();

            if (array.getComponentType() instanceof RTTITypeNumber<?>) {
                writer.value("<array of numbers>");
            } else {
                for (int i = 0, length = array.length(object); i < length; i++) {
                    serialize(array.get(object, i), array.getComponentType(), writer);
                }
            }

            writer.endArray();
        } else if (object instanceof String value) {
            writer.value(value);
        } else if (object instanceof Number value) {
            writer.value(value);
        } else if (object instanceof Boolean value) {
            writer.value(value);
        } else if (object instanceof RTTITypeEnum.Constant constant) {
            writer.value(constant.name());
        } else {
            writer.value("<unsupported type '" + type.getFullTypeName() + "'>");
        }
    }
}
