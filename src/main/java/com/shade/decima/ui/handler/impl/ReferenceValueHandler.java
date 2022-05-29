package com.shade.decima.ui.handler.impl;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.handler.ValueHandler;

public class ReferenceValueHandler implements ValueHandler {
    public static final ReferenceValueHandler INSTANCE = new ReferenceValueHandler();

    private ReferenceValueHandler() {
    }

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        final RTTIReference ref = (RTTIReference) value;
        final String path = ref.getPath();
        final RTTIObject uuid = ref.getUuid();

        if (path != null && uuid != null) {
            return "path = %s, uuid = %s".formatted(path, GGUUIDValueHandler.INSTANCE.getInlineValue(uuid.getType(), uuid));
        } else if (uuid != null) {
            return "uuid = %s".formatted(GGUUIDValueHandler.INSTANCE.getInlineValue(uuid.getType(), uuid));
        } else {
            return "none";
        }
    }
}
