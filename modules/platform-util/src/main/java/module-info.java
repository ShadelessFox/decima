module platform.util {
    exports com.shade.util.io;
    exports com.shade.util;
    exports com.shade.util.hash.spi;
    exports com.shade.util.hash;

    requires org.lz4.java;

    uses com.shade.util.hash.spi.Hasher;
}