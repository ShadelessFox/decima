module decima.rtti.generator {
    requires static transitive java.compiler;
    requires static com.palantir.javapoet;
    requires static com.google.gson;

    requires decima.rtti;
    requires platform.util;

    exports com.shade.decima.rtti.generator;
}
