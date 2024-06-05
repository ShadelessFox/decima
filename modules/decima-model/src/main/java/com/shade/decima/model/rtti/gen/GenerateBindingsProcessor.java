package com.shade.decima.model.rtti.gen;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.Writer;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Set;

public class GenerateBindingsProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Messager messager = processingEnv.getMessager();

        for (TypeElement annotation : annotations) {
            final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);

            for (Element element : elements) {
                final GenerateBindings meta = element.getAnnotation(GenerateBindings.class);
                messager.printMessage(Diagnostic.Kind.NOTE, "Generating types for " + element + " using " + meta.source());

                final String name = meta.path() + '.' + meta.namespace();
                try (Writer writer = processingEnv.getFiler().createSourceFile(name, element).openWriter()) {
                    writer.write("package " + meta.path() + ";\n\n");
                    writer.write("import com.shade.decima.model.rtti.gen.*;\n");
                    writer.write("import com.shade.decima.model.rtti.gen.RTTI.*;\n\n");
                    writer.write("import java.math.*;\n");
                    writer.write("import java.util.*;\n");
                    writer.write("import javax.annotation.processing.Generated;\n\n");
                    writer.write("@SuppressWarnings(\"ALL\")\n");
                    writer.write("@Generated(value = \"%s\", date = \"%s\")\n".formatted(getClass().getName(), OffsetDateTime.now()));
                    writer.write("public class " + meta.namespace() + " {\n");
                    writer.write("\tprivate " + meta.namespace() + "() {}\n\n");

                    final TypeContext context = new TypeContext();
                    context.load(Path.of(meta.source()));
                    messager.printMessage(Diagnostic.Kind.NOTE, "Loaded " + context.types().size() + " types from " + meta.source());

                    final TypeGenerator generator = new TypeGenerator(writer, 1);
                    generator.generate(context);
                    generator.flush();
                    messager.printMessage(Diagnostic.Kind.NOTE, "Generated " + name + " successfully");

                    writer.write("}\n");
                } catch (Exception e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate types for " + element + " using " + meta.source());
                }
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
}
