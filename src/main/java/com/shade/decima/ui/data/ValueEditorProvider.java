package com.shade.decima.ui.data;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.ui.data.viewer.LocalizedTextResourceViewer;
import com.shade.decima.ui.data.viewer.texture.TextureViewer;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

// TODO: Use services instead
public class ValueEditorProvider {
    private ValueEditorProvider() {
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
