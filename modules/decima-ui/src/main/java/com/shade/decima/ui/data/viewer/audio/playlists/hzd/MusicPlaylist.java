package com.shade.decima.ui.data.viewer.audio.playlists.hzd;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.viewer.audio.Codec;
import com.shade.decima.ui.data.viewer.audio.Playlist;
import com.shade.decima.ui.data.viewer.audio.data.echo.EchoBank;
import com.shade.decima.ui.data.viewer.audio.data.echo.EchoBank.Chunk;
import com.shade.decima.ui.data.viewer.audio.data.mpeg.MpegFrameHeader;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MusicPlaylist implements Playlist {
    private static final Codec.Generic CODEC = new Codec.Generic("mp3");

    private final RTTIObject object;
    private final List<TrackInfo> tracks = new ArrayList<>();

    private int frameSize;
    private int frameDuration;

    public MusicPlaylist(@NotNull RTTIObject object) {
        this.object = object;

        ByteBuffer buffer = ByteBuffer.wrap(object.get("MusicData")).order(ByteOrder.LITTLE_ENDIAN);
        EchoBank bank = EchoBank.read(buffer);

        Chunk.Media.Entry[] entries = bank.get(Chunk.Type.MEDA).entries();
        String[] names = Arrays.stream(bank.get(Chunk.Type.STRL).names())
            .filter(name -> name.endsWith(".mp3"))
            .map(name -> name.substring(0, name.length() - 4))
            .toArray(String[]::new);

        if (names.length != entries.length) {
            throw new IllegalStateException("Mismatched names and entries");
        }

        long offset = 0;
        int stream = 0;

        for (int i = 0; i < names.length; i++) {
            Chunk.Media.Entry entry = entries[i];

            if (offset > entry.offset()) {
                stream += 1;
            }

            offset = entry.offset();
            tracks.add(new TrackInfo(names[i], stream, entry.offset(), entry.size()));
        }
    }

    @NotNull
    @Override
    public String getName(int index) {
        return tracks.get(index).name();
    }

    @NotNull
    @Override
    public Duration getDuration(@NotNull PackfileManager manager, int index) throws IOException {
        TrackInfo track = tracks.get(index);

        if (frameSize == 0) {
            HwDataSource dataSource = object.objs("DataSources")[track.index()].cast();
            ByteBuffer buffer = ByteBuffer.wrap(dataSource.getData(manager, track.offset(), 4));
            MpegFrameHeader frame = new MpegFrameHeader(buffer.getInt());

            // It's too expensive to calculate the frame size and duration for every track
            frameSize = frame.frameSize();
            frameDuration = frame.samplesPerFrame() * 1000 / frame.samplingRate();
        }

        return Duration.ofMillis((long) track.size() / frameSize * frameDuration);
    }

    @NotNull
    @Override
    public Codec getCodec(int index) {
        return CODEC;
    }

    @NotNull
    @Override
    public byte[] getData(@NotNull PackfileManager manager, int index) throws IOException {
        TrackInfo track = tracks.get(index);
        HwDataSource dataSource = object.objs("DataSources")[track.index()].cast();

        return dataSource.getData(manager, track.offset(), track.size());
    }

    @Override
    public int size() {
        return tracks.size();
    }

    private record TrackInfo(@NotNull String name, int index, int offset, int size) {}
}
