package com.shade.decima.ui.data;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.RTTITypeString;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.data.editor.EnumValueEditor;
import com.shade.decima.ui.data.editor.StringValueEditor;
import com.shade.decima.ui.data.viewer.LocalizedTextResourceViewer;

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

    @Nullable
    public static ValueViewer findValueViewer(@NotNull RTTIType<?> type) {
        if (type instanceof RTTITypeClass cls && cls.getTypeName().equals("LocalizedTextResource")) {
            return LocalizedTextResourceViewer.INSTANCE;
        }
        return null;
    }
}
