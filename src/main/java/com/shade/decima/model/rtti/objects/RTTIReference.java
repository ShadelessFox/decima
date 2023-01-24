package com.shade.decima.model.rtti.objects;

import com.shade.util.NotNull;

public sealed interface RTTIReference permits RTTIReference.None, RTTIReference.Internal, RTTIReference.External {
    None NONE = new None();

    record External(@NotNull Kind kind, @NotNull RTTIObject uuid, @NotNull String path) implements RTTIReference {}

    record Internal(@NotNull Kind kind, @NotNull RTTIObject uuid) implements RTTIReference {}

    record None() implements RTTIReference {}

    enum Kind {
        LINK,
        REFERENCE
    }
}
