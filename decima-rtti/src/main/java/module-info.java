module decima.rtti {
    requires transitive platform.util;

    requires org.objectweb.asm;

    exports com.shade.decima.rtti.data;
    exports com.shade.decima.rtti.factory;
    exports com.shade.decima.rtti.runtime;
    exports com.shade.decima.rtti;
    exports com.shade.decima.rtti.io;
}