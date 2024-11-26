module platform.util {
    exports com.shade.util.io;
    exports com.shade.util;
    exports com.shade.util.hash.spi;
    exports com.shade.util.hash;

    uses com.shade.util.hash.spi.Hasher;

    provides com.shade.util.hash.spi.Hasher
        with com.shade.util.hash.CRC32C.Provider, com.shade.util.hash.MurmurHash3.Provider;
}