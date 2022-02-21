package com.shade.decima.ui.data;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.RTTITypeString;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.data.impl.EnumValueEditor;
import com.shade.decima.ui.data.impl.StringValueEditor;

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
