package com.shade.decima.game.hfw.storage;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

final class LruWeakCache<K, V> {
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();
    private final LinkedHashMap<K, WeakValue<K, V>> map;

    public LruWeakCache(int maxSize) {
        this.map = new LinkedHashMap<>(maxSize + 1, 1, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, WeakValue<K, V>> eldest) {
                return size() > maxSize;
            }
        };
    }

    public void put(K key, V value) {
        processQueue();
        map.put(key, new WeakValue<>(key, value, queue));
    }

    public V get(K key) {
        processQueue();
        WeakValue<K, V> value = map.get(key);
        return value != null ? value.get() : null;
    }

    private void processQueue() {
        for (WeakValue<K, ?> ref; (ref = (WeakValue<K, ?>) queue.poll()) != null; ) {
            if (ref == map.get(ref.key)) {
                map.remove(ref.key);
            }
        }
    }

    private static class WeakValue<K, V> extends WeakReference<V> {
        private final K key;

        public WeakValue(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }
    }
}
