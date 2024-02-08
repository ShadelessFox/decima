package com.shade.decima.ui.data.viewer.audio.playlists;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.viewer.audio.Playlist;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.io.IOException;

public record LocalizedSoundPlaylist(@NotNull RTTIObject object) implements Playlist {
    @NotNull
    @Override
    public String getName(int index) {
        final var dataSource = object.objs("DataSources")[index].obj("DataSource").<HwDataSource>cast();
        return IOUtils.getFilename(dataSource.getLocation());
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
