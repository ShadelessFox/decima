import com.shade.decima.rtti.generator.GenerateBindingsProcessor;

import javax.annotation.processing.Processor;

module decima.rtti.generator {
    requires java.compiler;

    requires com.google.gson;
    requires com.squareup.javapoet;

    requires decima.rtti;

    provides Processor with GenerateBindingsProcessor;
}