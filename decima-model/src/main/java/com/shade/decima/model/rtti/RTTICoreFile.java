package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.util.NotNull;

import java.util.List;
import java.util.function.Consumer;

public interface RTTICoreFile {
    @NotNull
    List<RTTIObject> objects();

    void visitAllObjects(@NotNull String type, @NotNull Consumer<RTTIObject> consumer);

    <T> void visitAllObjects(@NotNull Class<T> type, @NotNull Consumer<T> consumer);
}
