module decima.platform {
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires com.google.gson;
    requires io.github.classgraph;
    requires java.desktop;
    requires org.slf4j;
    requires platform.util;

    exports com.shade.platform.model.app;
    exports com.shade.platform.model.data;
    exports com.shade.platform.model.messages;
    exports com.shade.platform.model.persistence;
    exports com.shade.platform.model.runtime;
    exports com.shade.platform.model.util;
    exports com.shade.platform.model;

    opens com.shade.platform.model.messages.impl;

    uses com.shade.platform.model.app.Application;
}
