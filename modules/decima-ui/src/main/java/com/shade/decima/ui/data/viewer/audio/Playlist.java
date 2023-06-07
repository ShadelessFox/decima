package com.shade.decima.ui.data.viewer.audio;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.util.NotNull;

import java.io.IOException;

public interface Playlist {
    @NotNull
    String getName(int index);

    @NotNull
    byte[] getData(@NotNull PackfileManager manager, int index) throws IOException;

    int size();
}
