package com.shade.decima.ui.data.viewer.audio.playlists;

import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.ui.data.viewer.audio.Playlist;
import com.shade.decima.ui.data.viewer.audio.wwise.*;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class WwiseBankPlaylist implements Playlist {
    private static final Logger log = LoggerFactory.getLogger(WwiseBankPlaylist.class);

    private final RTTIObject object;
    private final WwiseBank bank;
    private final AkHircNode[] nodes;

    public WwiseBankPlaylist(@NotNull RTTIObject object) {
        final var size = object.i32("BankSize");
        final var data = object.<byte[]>get("BankData");
        final var buffer = ByteBuffer.wrap(data, 0, size).order(ByteOrder.LITTLE_ENDIAN);

        this.object = object;
        this.bank = WwiseBank.read(buffer);

        if (bank.has(WwiseBank.Chunk.Type.HIRC)) {
            this.nodes = Arrays.stream(bank.get(WwiseBank.Chunk.Type.HIRC).nodes())
                .filter(WwiseBankPlaylist::isPlayable)
                .toArray(AkHircNode[]::new);
        } else {
            this.nodes = new AkHircNode[0];
        }
    }

    @NotNull
    @Override
    public String getName(int index) {
        return "%d.wem".formatted(Integer.toUnsignedLong(nodes[index].id()));
    }

    @NotNull
    @Override
    public byte[] getData(@NotNull PackfileManager manager, int index) throws IOException {
        final AkHircNode node = nodes[index];
        final AkBankSourceData source;

        if (node instanceof AkSound sound) {
            source = sound.source();
        } else if (node instanceof AkMusicTrack track) {
            if (track.sources().length > 1) {
                log.warn("Track {} has {} sources, using the first one", track.id(), track.sources().length);
            }

            source = track.sources()[0];
        } else {
            throw new IllegalStateException();
        }

        return switch (source.type()) {
            case STREAMING, PREFETCH_STREAMING -> {
                final var dataSourceIndex = IOUtils.indexOf(object.ints("WemIDs"), source.info().sourceId());
                final var dataSource = object.objs("DataSources")[dataSourceIndex].<HwDataSource>cast();
                yield dataSource.getData(manager);
            }
            case DATA -> {
                final var header = bank.get(WwiseBank.Chunk.Type.DIDX).get(source.info().sourceId());
                yield Arrays.copyOfRange(bank.get(WwiseBank.Chunk.Type.DATA).data(), header.offset(), header.offset() + header.length());
            }
        };
    }

    @Override
    public int size() {
        return nodes.length;
    }

    private static boolean isPlayable(@NotNull AkHircNode node) {
        final AkBankSourceData source;

        if (node instanceof AkMusicTrack track && track.sources().length > 0) {
            source = track.sources()[0];
        } else if (node instanceof AkSound sound) {
            source = sound.source();
        } else {
            return false;
        }

        return source.info().inMemoryMediaSize() != 0;
    }
}
