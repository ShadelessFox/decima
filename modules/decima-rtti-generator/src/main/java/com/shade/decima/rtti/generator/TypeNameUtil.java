package com.shade.decima.rtti.generator;

import com.shade.decima.rtti.Ref;
import com.shade.decima.rtti.Value;
import com.shade.decima.rtti.generator.data.*;
import com.shade.util.NotNull;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.SourceVersion;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class TypeNameUtil {
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("(?<=[a-z])(?=[A-Z])");
    private static final Set<String> OBJECT_METHODS = Set.of(
        "hashCode",
        "equals",
        "clone",
        "notify",
        "notifyAll",
        "wait",
        "finalize"
    );

    private TypeNameUtil() {
    }

    @NotNull
    static String getJavaConstantName(@NotNull EnumTypeInfo info, @NotNull EnumValueInfo value) {
        if (true) {
            return "_" + info.values().indexOf(value);
        }

        String type = info.typeName().fullName();
        String name = value.name();

        if (name.toLowerCase(Locale.ROOT).startsWith(type.toLowerCase(Locale.ROOT))) {
            name = name.substring(type.length() + 1);
        }

        name = name.replaceAll("[()]", "").replaceAll("/", "_");
        name = Arrays.stream(CAMEL_CASE_PATTERN.split(name))
            .map(String::toUpperCase)
            .collect(Collectors.joining("_"));

        if (name.contains(" ")) {
            name = String.join("_", name.split(" ")).toUpperCase(Locale.ROOT);
        }

        if (name.isEmpty()) {
            name = "0";
        }

        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            name = '_' + name;
        }

        if (SourceVersion.isIdentifier(name)) {
            return name.toUpperCase(Locale.ROOT);
        }

        throw new IllegalStateException("Invalid constant name: " + name);
    }

    @NotNull
    static String getJavaPropertyName(@NotNull String name) {
        if (name.length() > 1 && name.matches("^m[A-Z].*$")) {
            name = name.substring(1);
        }
        if (name.indexOf(1, '_') > 0) {
            name = Arrays.stream(name.split("_"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining());
        }
        if (name.chars().allMatch(Character::isUpperCase)) {
            name = name.toLowerCase(Locale.ROOT);
        } else {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        if (!SourceVersion.isIdentifier(name)) {
            name = '_' + name;
        }
        if (SourceVersion.isIdentifier(name)) {
            if (!SourceVersion.isName(name) || OBJECT_METHODS.contains(name)) {
                name += '_';
            }
            return name;
        }
        throw new IllegalStateException("Invalid method name: " + name);
    }

    @NotNull
    static ClassName getTypeName(@NotNull ClassTypeInfo type, @NotNull TypeGenerator generator) {
        return (ClassName) getTypeName(type, generator, false);
    }

    @NotNull
    static TypeName getTypeName(@NotNull TypeInfo type, @NotNull TypeGenerator generator) {
        return getTypeName(type, generator, false);
    }

    @NotNull
    static TypeName getTypeName(@NotNull TypeInfo type, @NotNull TypeGenerator generator, boolean useWrapperType) {
        if (type instanceof EnumTypeInfo && useWrapperType) {
            return ParameterizedTypeName.get(ClassName.get(Value.class), getTypeName(type, generator, false));
        } else if (type instanceof ClassTypeInfo || type instanceof EnumTypeInfo) {
            return ClassName.get("" /* same package */, getJavaTypeName(type));
        } else if (type instanceof AtomTypeInfo atom) {
            if (atom.parent() != null) {
                return getTypeName(atom.parent(), generator, false);
            }
            TypeMirror builtin = generator.getBuiltin(atom.name());
            if (builtin != null) {
                return TypeName.get(builtin);
            }
            throw new IllegalArgumentException("Unknown atom type: " + atom.name());
        } else if (type instanceof PointerTypeInfo pointer) {
            return ParameterizedTypeName.get(ClassName.get(Ref.class), getTypeName(pointer.type().value(), generator, false).box());
        } else if (type instanceof ContainerTypeInfo container) {
            TypeName argument = getTypeName(container.type().value(), generator, false);
            if (argument.isPrimitive()) {
                return ArrayTypeName.of(argument);
            } else {
                return ParameterizedTypeName.get(ClassName.get(List.class), argument);
            }
        }
        throw new IllegalArgumentException("Unknown type: " + type.typeName());
    }

    @NotNull
    private static String getJavaTypeName(@NotNull TypeInfo info) {
        String name = info.typeName().fullName();
        if (info instanceof EnumTypeInfo && name.matches("^[e][A-Z].*$")) {
            return 'E' + name.substring(1);
        }
        return name;
    }
}
