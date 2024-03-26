package com.shade.decima.ui.data.viewer.audio;

import com.shade.util.NotNull;

public sealed interface Codec {
    record Wwise() implements Codec {}

    record Wave(@NotNull String encoding) implements Codec {}
}
