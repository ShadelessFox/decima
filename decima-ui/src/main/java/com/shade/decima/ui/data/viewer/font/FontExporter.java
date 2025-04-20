package com.shade.decima.ui.data.viewer.font;

import com.shade.decima.model.rtti.types.java.HwFont;
import com.shade.util.NotNull;

import java.nio.channels.WritableByteChannel;

public interface FontExporter {
    void export(@NotNull HwFont font, @NotNull WritableByteChannel channel) throws Exception;

    @NotNull
    String getExtension();
}
