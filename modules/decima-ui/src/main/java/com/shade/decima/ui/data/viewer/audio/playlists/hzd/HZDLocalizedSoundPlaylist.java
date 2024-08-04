package com.shade.decima.ui.data.viewer.audio.playlists.hzd;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.viewer.audio.AudioPlayerUtils;
import com.shade.decima.ui.data.viewer.audio.Codec;
import com.shade.decima.ui.data.viewer.audio.Playlist;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.io.IOException;
import java.time.Duration;

public record HZDLocalizedSoundPlaylist(@NotNull RTTIObject object) implements Playlist {
    @NotNull
    @Override
    public String getName(int index) {
        final var dataSource = object.objs("DataSources")[index].obj("DataSource").<HwDataSource>cast();
        return IOUtils.getFilename(dataSource.getLocation());
    }

    @NotNull
    @Override
    public Duration getDuration(@NotNull PackfileManager manager, int index) {
        return AudioPlayerUtils.getDuration(
            object.objs("DataSources")[index].i64("SampleCount"),
            object.obj("WaveData").i32("SampleRate")
        );
    }

    @NotNull
    @Override
    public Codec getCodec(int index) {
        return new Codec.Generic(object.obj("WaveData").str("Encoding").toLowerCase());
    }

    @NotNull
    @Override
    public byte[] getData(@NotNull PackfileManager manager, int index) throws IOException {
        final var dataSource = object.objs("DataSources")[index].obj("DataSource").<HwDataSource>cast();
        return dataSource.getData(manager);
    }

    @Override
    public int size() {
        return object.objs("DataSources").length;
    }
}
