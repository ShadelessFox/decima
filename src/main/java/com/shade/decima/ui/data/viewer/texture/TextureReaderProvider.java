package com.shade.decima.ui.data.viewer.texture;

import com.shade.util.NotNull;

public interface TextureReaderProvider {
    @NotNull
    TextureReader create(int width, int height, @NotNull String format);

    boolean supports(@NotNull String format);
}
