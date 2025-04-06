package com.shade.decima.game;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface Archive<K extends AssetId, V extends Asset> extends Closeable {
    Optional<V> get(K id);

    byte[] read(K id) throws IOException;

    List<V> assets();

    default boolean contains(K id) {
        return get(id).isPresent();
    }
}
