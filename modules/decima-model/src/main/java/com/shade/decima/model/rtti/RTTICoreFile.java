package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface RTTICoreFile {
    @NotNull
    List<RTTIObject> objects();

    @Nullable
    RTTIObject findObject(@NotNull Predicate<RTTIObject> predicate);

    void visitAllObjects(@NotNull String type, @NotNull Consumer<RTTIObject> consumer);

    <T> void visitAllObjects(@NotNull Class<T> type, @NotNull Consumer<T> consumer);
}
