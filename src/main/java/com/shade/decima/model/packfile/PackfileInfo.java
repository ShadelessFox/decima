package com.shade.decima.model.packfile;

import com.shade.decima.model.rtti.objects.Language;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public record PackfileInfo(@NotNull String name, @Nullable Language lang) {
}
