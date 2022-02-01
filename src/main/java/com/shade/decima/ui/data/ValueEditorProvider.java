package com.shade.decima.ui.data;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.types.RTTITypeEnum;
import com.shade.decima.rtti.types.RTTITypeString;
import com.shade.decima.ui.data.impl.EnumValueEditor;
import com.shade.decima.ui.data.impl.StringValueEditor;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

// TODO: Use services instead
public class ValueEditorProvider {
    private ValueEditorProvider() {
    }

    @Nullable
    public static ValueEditor findValueEditor(@NotNull RTTIType<?> type) {
        if (type instanceof RTTITypeString) {
            return StringValueEditor.INSTANCE;
        } else if (type instanceof RTTITypeEnum) {
            return EnumValueEditor.INSTANCE;
        } else {
            return null;
        }
    }
}
