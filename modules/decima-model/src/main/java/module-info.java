module decima.model {
    requires com.google.gson;
    requires decima.platform;
    requires java.desktop;
    requires org.slf4j;
    requires io.github.classgraph;

    exports com.shade.decima.model.app;
    exports com.shade.decima.model.archive;
    exports com.shade.decima.model.base;
    exports com.shade.decima.model.build;
    exports com.shade.decima.model.packfile.edit;
    exports com.shade.decima.model.packfile.prefetch;
    exports com.shade.decima.model.packfile.resource;
    exports com.shade.decima.model.packfile;
    exports com.shade.decima.model.rtti.messages.ds;
    exports com.shade.decima.model.rtti.objects;
    exports com.shade.decima.model.rtti.path;
    exports com.shade.decima.model.rtti.registry;
    exports com.shade.decima.model.rtti.types.ds;
    exports com.shade.decima.model.rtti.types.hzd;
    exports com.shade.decima.model.rtti.types.java;
    exports com.shade.decima.model.rtti.types;
    exports com.shade.decima.model.rtti;
    exports com.shade.decima.model.util.graph.impl;
    exports com.shade.decima.model.util.graph;
    exports com.shade.decima.model.util.hash.spi;
    exports com.shade.decima.model.util.hash;
    exports com.shade.decima.model.util;

    opens com.shade.decima.model.app;
    opens com.shade.decima.model.rtti.messages.ds;
    opens com.shade.decima.model.rtti.messages.dsdc;
    opens com.shade.decima.model.rtti.messages.hzd;
    opens com.shade.decima.model.rtti.messages.shared;
    exports com.shade.decima.model.packfile.oodle;

    uses com.shade.decima.model.rtti.registry.RTTITypeProvider;
    uses com.shade.decima.model.util.hash.spi.Hasher;

    provides com.shade.decima.model.rtti.registry.RTTITypeProvider with
        com.shade.decima.model.rtti.registry.providers.InternalTypeProvider,
        com.shade.decima.model.rtti.registry.providers.ExternalTypeProvider,
        com.shade.decima.model.rtti.registry.providers.JavaTypeProvider;

    provides com.shade.decima.model.util.hash.spi.Hasher with
        com.shade.decima.model.util.hash.CRC32C.Provider,
        com.shade.decima.model.util.hash.MurmurHash3.Provider;
}