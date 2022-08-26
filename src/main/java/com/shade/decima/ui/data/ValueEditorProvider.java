package com.shade.decima.ui.data;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.RTTITypeString;
import com.shade.decima.ui.data.editor.EnumValueEditor;
import com.shade.decima.ui.data.editor.StringValueEditor;
import com.shade.decima.ui.data.viewer.LocalizedTextResourceViewer;
import com.shade.decima.ui.data.viewer.TextureViewer;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

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
        if (type instanceof RTTITypeClass cls) {
            return switch (cls.getTypeName()) {
                case "LocalizedTextResource" -> LocalizedTextResourceViewer.INSTANCE;
                case "Texture" -> TextureViewer.INSTANCE;
                default -> null;
            };
        }
        return null;
    }
}
