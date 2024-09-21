package com.shade.decima.rtti.generator;

import com.shade.decima.rtti.generator.data.ClassTypeInfo;
import com.shade.decima.rtti.generator.data.TypeInfo;
import com.shade.util.NotNull;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GenerateBindingsProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = processingEnv.getMessager();
        var types = processingEnv.getTypeUtils();

        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateBindings.class)) {
            var annotation = element.getAnnotation(GenerateBindings.class);
            var packageName = String.valueOf(((QualifiedNameable) element).getQualifiedName());
            var className = packageName + "." + annotation.namespace();

            messager.printMessage(Diagnostic.Kind.NOTE, "generating type bindings " + className + " using " + annotation.source());

            try {
                var context = new TypeContext();
                context.load(getModuleRoot().resolve(annotation.source()));

                // Report missing callbacks right away. May be replaced with errors later on
                reportMissingCallbacks(context, annotation);

                var generator = new TypeGenerator();
                for (GenerateBindings.Callback callback : annotation.callbacks()) {
                    // Shenanigans to access <T> of ExtraBinaryDataCallback
                    var handlerType = (TypeElement) types.asElement(getAnnotationValueMirror(callback, GenerateBindings.Callback::handler));
                    var handlerParent = (DeclaredType) handlerType.getInterfaces().get(0);
                    var holderType = types.asElement(handlerParent.getTypeArguments().get(0));
                    generator.addCallback(callback.type(), handlerType, holderType);
                }

                var builder = TypeSpec.interfaceBuilder(annotation.namespace())
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "all").build());

                for (TypeInfo info : context.types()) {
                    if (info instanceof ClassTypeInfo cls && cls.isAssignableTo("ExportedSymbolGroup")) {
                        continue;
                    }
                    TypeSpec spec = generator.generate(info);
                    if (spec == null) {
                        continue;
                    }
                    builder.addType(spec);
                }

                try (Writer writer = processingEnv.getFiler().createSourceFile(className, element).openWriter()) {
                    JavaFile.builder(packageName, builder.build())
                        .addFileComment("This file was autogenerated. Do not edit!")
                        .build().writeTo(writer);
                }

                messager.printMessage(Diagnostic.Kind.NOTE, "generated successfully");
            } catch (Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "failed to generate: " + e);
            }
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(GenerateBindings.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void reportMissingCallbacks(@NotNull TypeContext context, @NotNull GenerateBindings annotation) {
        Set<String> typesMissingCallback = context.types().stream()
            .filter(info -> info instanceof ClassTypeInfo cls && cls.messages().contains("MsgReadBinary"))
            .map(TypeInfo::fullName)
            .collect(Collectors.toSet());

        for (GenerateBindings.Callback callback : annotation.callbacks()) {
            typesMissingCallback.remove(callback.type());
        }

        for (String type : typesMissingCallback) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.WARNING,
                "no extra binary data callback for " + annotation.namespace() + "." + type
            );
        }
    }

    @NotNull
    private Path getModuleRoot() throws IOException {
        var file = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "dummy");
        var path = Path.of(file.toUri());
        while (Files.notExists(path.resolve("build.gradle"))) {
            path = path.getParent();
        }
        return path;
    }

    @NotNull
    private static <A extends Annotation> TypeMirror getAnnotationValueMirror(
        @NotNull A annotation,
        @NotNull Function<A, Class<?>> extractor
    ) {
        try {
            Class<?> ignored = extractor.apply(annotation);
            // ^ this will always fail
            throw new AssertionError();
        } catch (MirroredTypeException e) {
            return Objects.requireNonNull(e.getTypeMirror());
        }
    }
}