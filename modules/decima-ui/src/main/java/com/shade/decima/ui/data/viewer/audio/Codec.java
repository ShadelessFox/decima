package com.shade.decima.ui.data.viewer.audio;

import com.shade.util.NotNull;

public sealed interface Codec {
    record Wem() implements Codec {}

    record Generic(@NotNull String name) implements Codec {}
}
