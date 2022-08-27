package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.util.NotNull;

public interface ImageReaderProvider {
    @NotNull
    ImageReader create(@NotNull String format);

    boolean supports(@NotNull String format);
}
