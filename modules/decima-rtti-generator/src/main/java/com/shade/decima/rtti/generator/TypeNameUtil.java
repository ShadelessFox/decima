package com.shade.decima.rtti.generator;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.data.Value;
import com.shade.decima.rtti.generator.data.*;
import com.shade.util.NotNull;

import javax.lang.model.SourceVersion;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

class TypeNameUtil {
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
        if (SourceVersion.isIdentifier(value.name()) && !SourceVersion.isKeyword(value.name())) {
            return value.name();
        } else {
            return "_" + info.values().indexOf(value);
        }
    }

    @NotNull
    static String getJavaPropertyName(@NotNull String name) {
        if (name.length() > 1 && name.matches("^m[A-Z].*$")) {
            name = name.substring(1);
        }
        if (name.indexOf(1, '_') > 0) {
            name = Arrays.stream(name.split("_"))
                .filter(part -> !part.isBlank())
                .map(TypeNameUtil::capitalize)
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
        if (type instanceof EnumTypeInfo enumeration && useWrapperType) {
            ParameterizedTypeName name = ParameterizedTypeName.get(ClassName.get(Value.class), getTypeName(type, generator, false));
            if (enumeration.flags()) {
                return ParameterizedTypeName.get(ClassName.get(Set.class), name);
            } else {
                return name;
            }
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
        return capitalize(info.typeName().fullName());
    }

    @NotNull
    private static String capitalize(@NotNull String name) {
        if (name.length() >= 2 && Character.isLowerCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }
}
