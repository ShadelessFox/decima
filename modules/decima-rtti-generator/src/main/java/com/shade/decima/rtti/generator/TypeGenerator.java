package com.shade.decima.rtti.generator;

import com.palantir.javapoet.*;
import com.shade.decima.rtti.Attr;
import com.shade.decima.rtti.Base;
import com.shade.decima.rtti.Category;
import com.shade.decima.rtti.Serializable;
import com.shade.decima.rtti.data.ExtraBinaryDataHolder;
import com.shade.decima.rtti.data.Value;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.decima.rtti.generator.data.*;
import com.shade.decima.rtti.runtime.TypedObject;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.io.BinaryReader;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TypeGenerator {
    private static final String NO_CATEGORY = "";

    private final Map<String, TypeMirror> callbacks = new HashMap<>();
    private final Map<String, TypeMirror> builtins = new HashMap<>();
    private final Map<String, TypeMirror> extensions = new HashMap<>();

    void addCallback(@NotNull String targetType, @NotNull TypeMirror handlerType) {
        if (callbacks.containsKey(targetType)) {
            throw new IllegalArgumentException("Callback for type '" + targetType + "' already exists");
        }
        callbacks.put(targetType, handlerType);
    }

    void addBuiltin(@NotNull String typeName, @NotNull TypeMirror javaType) {
        if (builtins.containsKey(typeName)) {
            throw new IllegalArgumentException("Builtin for type '" + typeName + "' already exists");
        }
        builtins.put(typeName, javaType);
    }

    void addExtension(@NotNull String typeName, @NotNull TypeMirror javaType) {
        if (extensions.containsKey(typeName)) {
            throw new IllegalArgumentException("Extension for type '" + typeName + "' already exists");
        }
        extensions.put(typeName, javaType);
    }

    @Nullable
    TypeSpec generate(@NotNull TypeInfo type) {
        return switch (type) {
            case ClassTypeInfo t -> generateClass(t);
            case EnumTypeInfo t -> generateEnum(t);
            case AtomTypeInfo ignored -> null;
            case ContainerTypeInfo ignored -> null;
            case PointerTypeInfo ignored -> null;
        };
    }

    @Nullable
    TypeMirror getBuiltin(@NotNull String name) {
        return builtins.get(name);
    }

    @NotNull
    private TypeSpec generateClass(@NotNull ClassTypeInfo info) {
        var categories = collectCategories(info);
        var root = categories.remove(NO_CATEGORY);

        var name = TypeNameUtil.getTypeName(info, this);
        var builder = TypeSpec.interfaceBuilder(name)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addSuperinterfaces(info.bases().stream().map(this::generateBase).toList())
            .addSuperinterface(TypedObject.class)
            .addAnnotation(AnnotationSpec.builder(Serializable.class)
                .addMember("version", "$L", info.version())
                .addMember("flags", "$L", info.flags())
                .build())
            .addTypes(categories.values().stream().map(this::generateCategory).toList())
            .addMethods(root != null ? generateAttrs(root.attrs) : List.of())
            .addMethods(categories.values().stream().map(this::generateCategoryAttr).toList());

        if (info.messages().contains("MsgReadBinary")) {
            var callback = callbacks.get(info.name());
            if (callback != null) {
                var deserialize = MethodSpec.methodBuilder("deserialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addAnnotation(Override.class)
                    .addParameter(BinaryReader.class, "reader")
                    .addParameter(TypeFactory.class, "factory")
                    .addException(IOException.class)
                    .addCode("new $T().deserialize(reader, factory, this);", callback);

                builder.addMethod(deserialize.build());
            }

            builder.addSuperinterface(ExtraBinaryDataHolder.class);
        }

        var extension = extensions.get(info.name());
        if (extension != null) {
            builder.addSuperinterface(TypeName.get(extension));
        }

        return builder.build();
    }

    @NotNull
    private TypeSpec generateCategory(@NotNull CategoryInfo category) {
        return TypeSpec.interfaceBuilder(category.name + "Category")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addAnnotation(Serializable.class)
            .addSuperinterfaces(category.bases.stream()
                .map(x -> TypeNameUtil.getTypeName(x, this).nestedClass(category.javaTypeName()))
                .toList())
            .addMethods(generateAttrs(category.attrs))
            .build();
    }

    @NotNull
    private List<MethodSpec> generateAttrs(@NotNull List<ClassAttrInfo> attrs) {
        List<MethodSpec> methods = new ArrayList<>(attrs.size());
        for (ClassAttrInfo attr : attrs) {
            // NOTE: Consider skipping save-state attributes here
            methods.add(generateGetterAttr(attr));
            methods.add(generateSetterAttr(attr));
        }
        return methods;
    }

    @NotNull
    private MethodSpec generateCategoryAttr(@NotNull CategoryInfo category) {
        var builder = MethodSpec.methodBuilder(TypeNameUtil.getJavaPropertyName(category.name))
            .addAnnotation(AnnotationSpec.builder(Category.class)
                .addMember("name", "$S", category.name)
                .build())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(ClassName.get("" /* same package */, category.javaTypeName()));
        if (!category.bases.isEmpty()) {
            builder.addAnnotation(Override.class);
        }
        return builder.build();
    }

    @NotNull
    private MethodSpec generateGetterAttr(@NotNull ClassAttrInfo attr) {
        var builder = AnnotationSpec.builder(Attr.class)
            .addMember("name", "$S", attr.name())
            .addMember("type", "$S", attr.type().typeName())
            .addMember("position", "$L", attr.position())
            .addMember("offset", "$L", attr.offset());
        if (attr.flags() != 0) {
            builder.addMember("flags", "$L", attr.flags());
        }
        if (attr.min() != null) {
            builder.addMember("min", "$S", attr.min());
        }
        if (attr.max() != null) {
            builder.addMember("max", "$S", attr.max());
        }
        return MethodSpec
            .methodBuilder(TypeNameUtil.getJavaPropertyName(attr.name()))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotation(builder.build())
            .returns(TypeNameUtil.getTypeName(attr.type().value(), this, true))
            .build();
    }

    @NotNull
    private MethodSpec generateSetterAttr(@NotNull ClassAttrInfo attr) {
        return MethodSpec
            .methodBuilder(TypeNameUtil.getJavaPropertyName(attr.name()))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(TypeNameUtil.getTypeName(attr.type().value(), this, true), "value")
            .returns(TypeName.VOID)
            .build();
    }

    @NotNull
    private TypeName generateBase(@NotNull ClassBaseInfo base) {
        return TypeNameUtil.getTypeName(base.type(), this)
            .annotated(AnnotationSpec.builder(Base.class)
                .addMember("offset", "$L", base.offset())
                .build());
    }

    @NotNull
    private TypeSpec generateEnum(@NotNull EnumTypeInfo info) {
        var name = (ClassName) TypeNameUtil.getTypeName(info, this);
        var builder = TypeSpec.enumBuilder(name)
            .addSuperinterface(ParameterizedTypeName.get(ClassName.get(info.flags() ? Value.OfEnumSet.class : Value.OfEnum.class), name))
            .addAnnotation(AnnotationSpec.builder(Serializable.class)
                .addMember("size", "$L", info.size().bytes())
                .build())
            .addField(String.class, "name", Modifier.PRIVATE, Modifier.FINAL)
            .addField(info.size().type(), "value", Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder()
                .addParameter(String.class, "name")
                .addParameter(int.class, "value")
                .addCode("this.name = name;\nthis.value = ($T) value;", info.size().type())
                .build());
        if (!info.flags()) {
            builder.addMethod(MethodSpec.methodBuilder("valueOf")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(info.size().type(), "value")
                .returns(name)
                .addStatement("return ($T) $T.valueOf($T.class, $L)", name, Value.class, name, "value")
                .build());
            builder.alwaysQualify(Value.class.getSimpleName());
            builder.avoidClashesWithNestedClasses(Value.class);
        }
        builder.addMethod(MethodSpec.methodBuilder("value")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $L", "value")
                .build())
            .addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $L", "name")
                .build())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        for (EnumValueInfo value : info.values()) {
            builder.addEnumConstant(
                TypeNameUtil.getJavaConstantName(info, value),
                TypeSpec.anonymousClassBuilder("$S, $L", value.name(), value.value()).build()
            );
        }
        return builder.build();
    }

    @NotNull
    private static Map<String, CategoryInfo> collectCategories(@NotNull ClassTypeInfo info) {
        var inheritedCategories = collectAllCategories(info).stream()
            .filter(category -> findCategoryHost(info, category) == info)
            .map(category -> Map.entry(category, findInheritedCategories(info, category)))
            .filter(pair -> !pair.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var declaredCategories = info.attrs().stream()
            .collect(Collectors.groupingBy(
                attr -> Objects.requireNonNullElse(attr.category(), NO_CATEGORY),
                LinkedHashMap::new,
                Collectors.toList()
            ));
        return Stream.concat(inheritedCategories.keySet().stream(), declaredCategories.keySet().stream())
            .distinct()
            .collect(Collectors.toMap(
                Function.identity(),
                category -> new CategoryInfo(
                    category,
                    declaredCategories.getOrDefault(category, List.of()),
                    inheritedCategories.getOrDefault(category, List.of())
                ),
                (u, v) -> {
                    throw new IllegalStateException("Duplicate category: " + u.name);
                },
                LinkedHashMap::new
            ));
    }

    @NotNull
    private static Set<String> collectAllCategories(@NotNull ClassTypeInfo info) {
        var categories = new HashSet<String>();
        collectAllCategories(info, categories);
        return categories;
    }

    private static void collectAllCategories(@NotNull ClassTypeInfo info, @NotNull Set<String> categories) {
        for (ClassAttrInfo attr : info.attrs()) {
            var category = attr.category();
            if (category != null) {
                categories.add(category);
            }
        }
        for (ClassBaseInfo base : info.bases()) {
            collectAllCategories(base.type(), categories);
        }
    }

    @NotNull
    private static List<ClassTypeInfo> findInheritedCategories(@NotNull ClassTypeInfo info, @NotNull String name) {
        return info.bases().stream()
            .map(base -> findCategoryHost(base.type(), name))
            .filter(Objects::nonNull)
            .toList();
    }

    @Nullable
    private static ClassTypeInfo findCategoryHost(@NotNull ClassTypeInfo info, @NotNull String name) {
        for (ClassAttrInfo attr : info.attrs()) {
            if (name.equals(attr.category())) {
                return info;
            }
        }
        var matches = info.bases().stream()
            .map(base -> findCategoryHost(base.type(), name))
            .filter(Objects::nonNull)
            .limit(2)
            .toList();
        return switch (matches.size()) {
            case 0 -> null;
            case 1 -> matches.getFirst();
            // In cases where multiple bases have the same category interface,
            // this type must implement, a new interface that inherits from all
            // of them to make the Java compiler happy
            default -> info;
        };
    }

    private record CategoryInfo(
        @NotNull String name,
        @NotNull List<ClassAttrInfo> attrs,
        @NotNull List<ClassTypeInfo> bases
    ) {
        @NotNull
        private String javaTypeName() {
            return name + "Category";
        }
    }
}
