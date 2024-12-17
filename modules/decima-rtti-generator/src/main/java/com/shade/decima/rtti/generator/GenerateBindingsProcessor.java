package com.shade.decima.rtti.generator;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import com.shade.decima.rtti.factory.TypeName;
import com.shade.decima.rtti.generator.data.ClassTypeInfo;
import com.shade.decima.rtti.generator.data.TypeInfo;
import com.shade.util.NotNull;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
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
    private static final AnnotationSpec suppressWarningsAnnotation = AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$S", "all")
        .build();
    private static final AnnotationSpec generatedBindingsAnnotation = AnnotationSpec.builder(Generated.class)
        .addMember("value", "$S", GenerateBindingsProcessor.class.getName())
        .build();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = processingEnv.getMessager();
        var types = processingEnv.getTypeUtils();

        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateBindings.class)) {
            var module = (ModuleElement) element;
            var annotation = module.getAnnotation(GenerateBindings.class);

            var targetName = annotation.target();
            var className = targetName.substring(targetName.lastIndexOf('.') + 1);
            var packageName = targetName.substring(0, targetName.lastIndexOf('.'));
            var qualifiedName = module.getQualifiedName() + "/" + targetName;

            messager.printNote("generating type bindings " + targetName + " from " + annotation.source());

            try {
                var context = new TypeContext();
                try (var reader = Files.newBufferedReader(getModuleRoot().resolve(annotation.source()))) {
                    context.load(reader);
                }

                // Report missing callbacks right away. May be replaced with errors later on
                reportMissingCallbacks(context, annotation);

                var generator = new TypeGenerator();
                for (GenerateBindings.Callback callback : annotation.callbacks()) {
                    // Shenanigans to access <T> of ExtraBinaryDataCallback
                    var handlerType = (TypeElement) types.asElement(getAnnotationValueMirror(callback, GenerateBindings.Callback::handler));
                    var handlerParent = (DeclaredType) handlerType.getInterfaces().getFirst();
                    var holderType = types.asElement(handlerParent.getTypeArguments().getFirst());
                    generator.addCallback(callback.type(), handlerType.asType());
                    generator.addExtension(callback.type(), holderType.asType());
                }

                for (GenerateBindings.Builtin builtin : annotation.builtins()) {
                    var javaType = getAnnotationValueMirror(builtin, GenerateBindings.Builtin::javaType);
                    generator.addBuiltin(builtin.type(), javaType);
                }

                for (GenerateBindings.Extension extension : annotation.extensions()) {
                    var extensionType = getAnnotationValueMirror(extension, GenerateBindings.Extension::extension);
                    generator.addExtension(extension.type(), extensionType);
                }

                var builder = TypeSpec.interfaceBuilder(className)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(suppressWarningsAnnotation)
                    .addAnnotation(generatedBindingsAnnotation);

                for (TypeInfo info : context.types()) {
                    if (skipType(info)) {
                        continue;
                    }
                    TypeSpec spec = generator.generate(info);
                    if (spec == null) {
                        continue;
                    }
                    builder.addType(spec);
                }

                try (Writer writer = processingEnv.getFiler().createSourceFile(qualifiedName, element).openWriter()) {
                    JavaFile.builder(packageName, builder.build())
                        .indent("\t")
                        .addFileComment("This file was autogenerated. Do not edit!")
                        .build().writeTo(writer);
                }
            } catch (Exception e) {
                messager.printError("failed to generate: " + e);
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

    private static boolean skipType(@NotNull TypeInfo info) {
        return info instanceof ClassTypeInfo cls && cls.isAssignableTo("ExportedSymbolGroup");
    }

    private void reportMissingCallbacks(@NotNull TypeContext context, @NotNull GenerateBindings annotation) {
        Set<String> typesMissingCallback = context.types().stream()
            .filter(info -> info instanceof ClassTypeInfo cls && cls.messages().contains("MsgReadBinary"))
            .map(TypeInfo::typeName)
            .map(TypeName::fullName)
            .collect(Collectors.toSet());

        for (GenerateBindings.Callback callback : annotation.callbacks()) {
            typesMissingCallback.remove(callback.type());
        }

        for (String type : typesMissingCallback) {
            processingEnv.getMessager().printWarning("no extra binary data callback for " + annotation.target() + "." + type);
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
