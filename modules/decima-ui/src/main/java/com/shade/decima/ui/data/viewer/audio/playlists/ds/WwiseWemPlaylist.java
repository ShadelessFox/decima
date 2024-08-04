package com.shade.decima.ui.data.viewer.audio.playlists.ds;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.viewer.audio.Codec;
import com.shade.decima.ui.data.viewer.audio.Playlist;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.io.IOException;
import java.time.Duration;

public record WwiseWemPlaylist(@NotNull RTTIObject object) implements Playlist {
    @NotNull
    @Override
    public String getName(int index) {
        final var dataSource = object.obj("DataSource").<HwDataSource>cast();
        return IOUtils.getFilename(dataSource.getLocation());
    }

    @NotNull
    @Override
    public Duration getDuration(@NotNull PackfileManager manager, int index) {
        return Duration.ofMillis((long) (object.f32("mLengthInSeconds") * 1000L));
    }

    @NotNull
    @Override
    public Codec getCodec(int index) {
        return new Codec.Wem();
    }

    @NotNull
    @Override
    public byte[] getData(@NotNull PackfileManager manager, int index) throws IOException {
        final var dataSource = object.obj("DataSource").<HwDataSource>cast();
        return dataSource.getData(manager);
    }

    @Override
    public int size() {
        return 1;
    }
}
