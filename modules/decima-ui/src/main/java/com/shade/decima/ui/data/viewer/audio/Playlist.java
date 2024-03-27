package com.shade.decima.ui.data.viewer.audio;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.util.NotNull;

import java.io.IOException;
import java.time.Duration;

public interface Playlist {
    @NotNull
    String getName(int index);

    @NotNull
    Duration getDuration(@NotNull PackfileManager manager, int index) throws IOException;

    @NotNull
    Codec getCodec(int index);

    @NotNull
    byte[] getData(@NotNull PackfileManager manager, int index) throws IOException;

    int size();
}
