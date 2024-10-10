package com.shade.decima.rtti.generator;

import com.shade.decima.rtti.Value;
import com.shade.decima.rtti.generator.data.*;
import com.shade.decima.rtti.serde.DefaultExtraBinaryDataCallback;
import com.shade.decima.rtti.serde.ExtraBinaryDataCallback;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.squareup.javapoet.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static com.shade.decima.rtti.RTTI.*;

public class TypeGenerator {
    public static final String CALLBACK_FIELD_NAME = "EXTRA_BINARY_DATA_CALLBACK";

    private static final String NO_CATEGORY = "";

    private final Map<String, CallbackInfo> callbacks = new HashMap<>();

    public void addCallback(@NotNull String targetType, @NotNull Element handlerType, @NotNull Element holderType) {
        if (callbacks.containsKey(targetType)) {
            throw new IllegalArgumentException("Callback for type '" + targetType + "' already exists");
        }
        callbacks.put(targetType, new CallbackInfo(handlerType, holderType));
    }

    @Nullable
    public TypeSpec generate(@NotNull TypeInfo type) {
        if (type instanceof ClassTypeInfo t) {
            return generateClass(t);
        } else if (type instanceof EnumTypeInfo t) {
            return generateEnum(t);
        } else if (type instanceof AtomTypeInfo) {
            return null;
        } else if (type instanceof ContainerTypeInfo || type instanceof PointerTypeInfo) {
            return null;
        } else {
            throw new IllegalArgumentException("Unknown type: " + type.typeName());
        }
    }

    @NotNull
    private TypeSpec generateClass(@NotNull ClassTypeInfo info) {
        var categories = collectCategories(info);
        var root = categories.remove(NO_CATEGORY);

        var builder = TypeSpec.interfaceBuilder(TypeNameUtil.getTypeName(info))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addSuperinterfaces(info.bases().stream().map(this::generateBase).toList())
            .addAnnotation(AnnotationSpec.builder(Serializable.class)
                .addMember("version", "$L", info.version())
                .addMember("flags", "$L", info.flags())
                .build())
            .addTypes(categories.values().stream().map(this::generateCategory).toList())
            .addMethods(root != null ? generateAttrs(root.attrs) : List.of())
            .addMethods(categories.values().stream().map(this::generateCategoryAttr).toList());

        if (info.messages().contains("MsgReadBinary")) {
            var type = ParameterizedTypeName.get(ClassName.get(ExtraBinaryDataCallback.class), WildcardTypeName.subtypeOf(Object.class));
            var callback = callbacks.get(info.name());

            if (callback != null) {
                builder.addSuperinterface(callback.holderType.asType());
                builder.addField(FieldSpec.builder(type, CALLBACK_FIELD_NAME)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T()", callback.handlerType.asType())
                    .build());
            } else {
                builder.addSuperinterface(DefaultExtraBinaryDataCallback.MissingExtraData.class);
                builder.addField(FieldSpec.builder(type, CALLBACK_FIELD_NAME)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T()", DefaultExtraBinaryDataCallback.class)
                    .build());
            }
        }

        return builder.build();
    }

    @NotNull
    private TypeSpec generateCategory(@NotNull CategoryInfo category) {
        return TypeSpec.interfaceBuilder(category.name + "Category")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addAnnotation(Serializable.class)
            .addSuperinterfaces(category.bases.stream()
                .map(x -> TypeNameUtil.getTypeName(x).nestedClass(category.javaTypeName()))
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
            .returns(TypeNameUtil.getTypeName(attr.type().value(), true))
            .build();
    }

    @NotNull
    private MethodSpec generateSetterAttr(@NotNull ClassAttrInfo attr) {
        return MethodSpec
            .methodBuilder(TypeNameUtil.getJavaPropertyName(attr.name()))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(TypeNameUtil.getTypeName(attr.type().value(), true), "value")
            .returns(TypeName.VOID)
            .build();
    }

    @NotNull
    private TypeName generateBase(@NotNull ClassBaseInfo base) {
        return TypeNameUtil.getTypeName(base.type())
            .annotated(AnnotationSpec.builder(Base.class)
                .addMember("offset", "$L", base.offset())
                .build());
    }

    @NotNull
    private TypeSpec generateEnum(@NotNull EnumTypeInfo info) {
        var name = (ClassName) TypeNameUtil.getTypeName(info);
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
        var grouped = info.attrs().stream()
            .collect(Collectors.groupingBy(
                attr -> Objects.requireNonNullElse(attr.category(), NO_CATEGORY),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        return grouped.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new CategoryInfo(entry.getKey(), entry.getValue(), findCategories(entry.getKey(), info)),
                (u, v) -> {
                    throw new IllegalStateException("Duplicate category: " + u.name);
                },
                LinkedHashMap::new
            ));
    }

    @NotNull
    private static List<ClassTypeInfo> findCategories(@NotNull String name, @NotNull ClassTypeInfo info) {
        return info.bases().stream()
            .map(base -> findCategory(base.type(), name))
            .filter(Objects::nonNull)
            .toList();
    }

    @Nullable
    private static ClassTypeInfo findCategory(@NotNull ClassTypeInfo info, @NotNull String name) {
        for (ClassAttrInfo attr : info.attrs()) {
            if (name.equals(attr.category())) {
                return info;
            }
        }
        for (ClassBaseInfo base : info.bases()) {
            ClassTypeInfo result = findCategory(base.type(), name);
            if (result != null) {
                return result;
            }
        }
        return null;
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

    private record CallbackInfo(
        @NotNull Element handlerType,
        @NotNull Element holderType
    ) {
    }
}
