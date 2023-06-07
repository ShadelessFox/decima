package com.shade.decima.ui.data.viewer.audio.playlists;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.viewer.audio.Playlist;
import com.shade.util.NotNull;

import java.io.IOException;

public record WavePlaylist(@NotNull RTTIObject object) implements Playlist {
    @NotNull
    @Override
    public String getName(int index) {
        return object.str("Name");
    }

    @NotNull
    @Override
    public byte[] getData(@NotNull PackfileManager manager, int index) throws IOException {
        if (object.bool("IsStreaming")) {
            return object.obj("DataSource").<HwDataSource>cast().getData(manager);
        } else {
            return object.get("WaveData");
        }
    }

    @NotNull
    public String getCodec() {
        return object.str("Encoding").toLowerCase();
    }

    @Override
    public int size() {
        return 1;
    }
}
