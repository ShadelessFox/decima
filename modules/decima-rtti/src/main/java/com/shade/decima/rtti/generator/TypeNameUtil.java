package com.shade.decima.rtti.generator;

import com.shade.decima.rtti.Ref;
import com.shade.decima.rtti.data.*;
import com.shade.util.NotNull;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.SourceVersion;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class TypeNameUtil {
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("[A-Z][a-z]+");

    private TypeNameUtil() {
    }

    @NotNull
    static String getJavaConstantName(@NotNull EnumTypeInfo info, @NotNull String name) {
        if (name.toLowerCase(Locale.ROOT).startsWith(info.typeName().toLowerCase(Locale.ROOT))) {
            name = name.substring(info.typeName().length() + 1);
        }

        Matcher matcher = CAMEL_CASE_PATTERN.matcher(name);
        if (matcher.find()) {
            List<String> parts = new ArrayList<>();
            if (matcher.start() > 0) {
                parts.add(name.substring(0, matcher.start()));
            }
            do {
                parts.add(name.substring(matcher.start(), matcher.end()));
            } while (matcher.find());
            name = parts.stream().map(String::toUpperCase).collect(Collectors.joining("_"));
        }

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
        if (name.contains("_")) {
            name = Arrays.stream(name.split("_"))
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining());
        }
        if (name.chars().allMatch(Character::isUpperCase)) {
            name = name.toLowerCase(Locale.ROOT);
        } else {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        if (SourceVersion.isIdentifier(name)) {
            if (!SourceVersion.isName(name)) {
                name += '_';
            }
            return name;
        }
        throw new IllegalStateException("Invalid method name: " + name);
    }

    @NotNull
    static ClassName getTypeName(@NotNull ClassTypeInfo type) {
        return (ClassName) getTypeName((TypeInfo) type);
    }

    @NotNull
    static TypeName getTypeName(@NotNull TypeInfo type) {
        if (type instanceof ClassTypeInfo ||
            type instanceof EnumTypeInfo
        ) {
            return ClassName.get("" /* same package */, getJavaTypeName(type));
        } else if (type instanceof AtomTypeInfo atom) {
            if (atom.parent() != null) {
                return getTypeName(atom.parent());
            }
            return switch (atom.name()) {
                case "wchar" -> TypeName.CHAR;
                case "int8", "uint8" -> TypeName.BYTE;
                case "int16", "uint16" -> TypeName.SHORT;
                case "int32", "uint32", "int", "uint" -> TypeName.INT;
                case "int64", "uint64" -> TypeName.LONG;
                case "int128", "uint128" -> ClassName.get(BigInteger.class);
                case "float" -> TypeName.FLOAT;
                case "double" -> TypeName.DOUBLE;
                case "bool" -> TypeName.BOOLEAN;
                case "String", "WString" -> TypeName.get(String.class);
                default -> throw new IllegalArgumentException("Unknown atom type: " + atom.name());
            };
        } else if (type instanceof PointerTypeInfo pointer) {
            return ParameterizedTypeName.get(ClassName.get(Ref.class), getTypeName(pointer.type().value()));
        } else if (type instanceof ContainerTypeInfo container) {
            TypeName argument = getTypeName(container.type().value());
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
        String name = info.typeName();
        if (info instanceof EnumTypeInfo && name.matches("^[Ee][A-Z].*$")) {
            return name.substring(1);
        }
        return name;
    }
}