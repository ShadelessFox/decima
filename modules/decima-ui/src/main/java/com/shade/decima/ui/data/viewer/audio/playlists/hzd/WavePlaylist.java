package com.shade.decima.ui.data.viewer.audio.playlists.hzd;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.viewer.audio.AudioPlayerUtils;
import com.shade.decima.ui.data.viewer.audio.Codec;
import com.shade.decima.ui.data.viewer.audio.Playlist;
import com.shade.util.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

public record WavePlaylist(@NotNull RTTIObject object) implements Playlist {
    @NotNull
    @Override
    public String getName(int index) {
        Objects.checkIndex(index, 1);
        return object.str("Name");
    }

    @NotNull
    @Override
    public Duration getDuration(@NotNull PackfileManager manager, int index) {
        Objects.checkIndex(index, 1);
        return AudioPlayerUtils.getDuration(object.i32("SampleCount"), object.i32("SampleRate"));
    }

    @NotNull
    @Override
    public Codec getCodec(int index) {
        return new Codec.Wave(object.str("Encoding").toLowerCase());
    }

    @NotNull
    @Override
    public byte[] getData(@NotNull PackfileManager manager, int index) throws IOException {
        Objects.checkIndex(index, 1);
        if (object.bool("IsStreaming")) {
            return object.obj("DataSource").<HwDataSource>cast().getData(manager);
        } else {
            return object.get("WaveData");
        }
    }

    @Override
    public int size() {
        return 1;
    }
}
