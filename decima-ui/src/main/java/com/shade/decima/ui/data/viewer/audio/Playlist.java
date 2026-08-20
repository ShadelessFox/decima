package com.shade.decima.ui.data.viewer.audio;

import com.shade.decima.model.archive.ArchiveManager;
import com.shade.util.NotNull;

import java.io.IOException;
import java.time.Duration;

public interface Playlist {
    @NotNull
    String getName(int index);

    @NotNull
    Duration getDuration(@NotNull ArchiveManager manager, int index) throws IOException;

    @NotNull
    Codec getCodec(int index);

    @NotNull
    byte[] getData(@NotNull ArchiveManager manager, int index) throws IOException;

    int size();
}
