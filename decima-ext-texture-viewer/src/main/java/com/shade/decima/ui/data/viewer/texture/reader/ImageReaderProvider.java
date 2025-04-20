package com.shade.decima.ui.data.viewer.texture.reader;

import com.shade.decima.ui.data.viewer.texture.util.Channel;
import com.shade.util.NotNull;

import java.util.EnumSet;
import java.util.Set;

public interface ImageReaderProvider {
    @NotNull
    ImageReader create(@NotNull String format);

    boolean supports(@NotNull String format);

    @NotNull
    default Set<Channel> channels(@NotNull String format) {
        return EnumSet.allOf(Channel.class);
    }
}
