package com.shade.decima.app.ui;

import com.shade.decima.app.ui.tree.TreeStructure;
import com.shade.decima.rtti.data.Ref;
import com.shade.decima.rtti.runtime.*;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.List;
import java.util.stream.IntStream;

record ObjectStructure(
    @NotNull ClassTypeInfo info,
    @NotNull Object object
) implements TreeStructure<ObjectStructure.Element> {
    public sealed interface Element {
        @NotNull
        TypeInfo type();

        @Nullable
        Object value();

        record Class(
            @NotNull ClassTypeInfo type,
            @NotNull Object object
        ) implements Element {
            @Nullable
            @Override
            public Object value() {
                return object;
            }

            @Override
            public String toString() {
                return getDisplayString(this);
            }
        }

        record Attr(
            @NotNull Object object,
            @NotNull ClassAttrInfo attr
        ) implements Element {
            @NotNull
            @Override
            public TypeInfo type() {
                return attr.type().get();
            }

            @Nullable
            @Override
            public Object value() {
                return attr.get(object);
            }

            @Override
            public String toString() {
                return "%s = %s".formatted(attr.name(), getDisplayString(this));
            }
        }

        record Index(
            @NotNull Object container,
            @NotNull ContainerTypeInfo info,
            int index
        ) implements Element {
            @NotNull
            @Override
            public TypeInfo type() {
                return info.itemType().get();
            }

            @Nullable
            @Override
            public Object value() {
                return info.get(container, index);
            }

            @Override
            public String toString() {
                return "[%d] = %s".formatted(index, getDisplayString(this));
            }
        }
    }

    @NotNull
    @Override
    public Element getRoot() {
        return new Element.Class(info, object);
    }

    @NotNull
    @Override
    public List<? extends Element> getChildren(@NotNull Element parent) {
        var value = parent.value();
        if (value == null) {
            return List.of();
        }
        return getChildren(parent.type(), value);
    }

    @Override
    public boolean hasChildren(@NotNull Element node) {
        var value = node.value();
        if (value == null) {
            return false;
        }
        return hasChildren(node.type(), value);
    }

    @Override
    public String toString() {
        return "ObjectStructure[%s]".formatted(info.name());
    }

    @NotNull
    private List<? extends Element> getChildren(@NotNull TypeInfo info, @NotNull Object object) {
        return switch (info) {
            case ClassTypeInfo c -> c.displayableAttrs().stream()
                .map(attr -> new Element.Attr(object, attr))
                .toList();
            case ContainerTypeInfo c -> IntStream.range(0, c.length(object))
                .mapToObj(index -> new Element.Index(object, c, index))
                .toList();
            default -> throw new IllegalStateException();
        };
    }

    private boolean hasChildren(@NotNull TypeInfo info, @NotNull Object object) {
        return switch (info) {
            case ClassTypeInfo ignored -> true;
            case ContainerTypeInfo ignored -> true;
            default -> false;
        };
    }

    @NotNull
    private static String getDisplayString(@NotNull Element element) {
        var type = element.type().name().toString();
        var value = switch (element.type()) {
            case ClassTypeInfo ignored -> null;
            case ContainerTypeInfo container when element.value() != null ->
                "(%d items)".formatted(container.length(element.value()));
            case PointerTypeInfo ignored when element.value() instanceof Ref<?> ref && ref.get() instanceof TypedObject object ->
                "<%s>".formatted(object.getType().name());
            default -> String.valueOf(element.value());
        };
        if (value != null) {
            return "{%s} %s".formatted(type, value);
        } else {
            return "{%s}".formatted(type);
        }
    }
}
