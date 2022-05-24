package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.handler.ValueHandler;

public class GGUUIDValueHandler implements ValueHandler {
    public static final GGUUIDValueHandler INSTANCE = new GGUUIDValueHandler();

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        final RTTIObject object = (RTTIObject) value;

        return "{%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x}".formatted(
            object.<Byte>get("Data3"),
            object.<Byte>get("Data2"),
            object.<Byte>get("Data1"),
            object.<Byte>get("Data0"),
            object.<Byte>get("Data5"),
            object.<Byte>get("Data4"),
            object.<Byte>get("Data7"),
            object.<Byte>get("Data6"),
            object.<Byte>get("Data8"),
            object.<Byte>get("Data9"),
            object.<Byte>get("Data10"),
            object.<Byte>get("Data11"),
            object.<Byte>get("Data12"),
            object.<Byte>get("Data13"),
            object.<Byte>get("Data14"),
            object.<Byte>get("Data15")
        );
    }
}
