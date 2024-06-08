package com.shade.decima.model.rtti.gen;

import com.shade.decima.model.rtti.RTTITypeSerialized.TypeId;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TypeHasher {
    private final Map<TypeContext.Type, TypeId> cache = new HashMap<>();
    private final Set<TypeContext.Type> pending = new HashSet<>();

    @NotNull
    public String getTypeString(@NotNull TypeContext.Type type) {
        final StringBuilder sb = new StringBuilder()
            .append("RTTIBinaryVersion: 2, Type: ").append(getFullTypeName(type)).append('\n');

        if (type instanceof TypeContext.ClassType cls) {
            addTypeAttrInfo(sb, cls);
        }

        addTypeBaseInfo(sb, type, 0);

        if (type instanceof TypeContext.EnumType enumType) {
            if (!enumType.flags()) {
                sb.append("Enum-Size: %d\n".formatted(enumType.size()));
            }

            for (TypeContext.EnumValue value : enumType.values()) {
                sb.append("Enumeration-Value: %s %s\n".formatted(value.value(), value.name()));
            }
        }

        if (type instanceof TypeContext.TemplateType templateType && isContainer(type)) {
            sb.append("Contained-Type: %s\n".formatted(getHashString(getNestedTypeId(templateType.argument()))));
        }

        return sb.toString();
    }

    private static boolean isContainer(@NotNull TypeContext.Type type) {
        return type.name().equals("Array") || type.name().equals("HashMap") || type.name().equals("HashSet");
    }

    @NotNull
    public TypeId getTypeId(@NotNull TypeContext.Type type) {
        if (cache.containsKey(type)) {
            return cache.get(type);
        }

        try {
            pending.add(type);

            final String string = getTypeString(type);
            final TypeId id = getTypeId(string);

            cache.put(type, id);

            return id;
        } finally {
            pending.remove(type);
        }
    }

    @NotNull
    private TypeId getNestedTypeId(@NotNull TypeContext.TypeRef<?> ref) {
        final TypeContext.Type type = ref.get();
        if (pending.contains(type)) {
            return getShortTypeId(type);
        } else {
            return getTypeId(type);
        }
    }

    @NotNull
    private TypeId getShortTypeId(@NotNull TypeContext.Type type) {
        return getShortTypeId(getFullTypeName(type));
    }

    @NotNull
    private TypeId getShortTypeId(@NotNull String name) {
        return getTypeId("RTTIBinaryVersion: 2, Type: " + name);
    }

    @NotNull
    private TypeId getTypeId(@NotNull String string) {
        final long[] hash = MurmurHash3.mmh3(string.getBytes(StandardCharsets.UTF_8));
        return new TypeId(hash[0], hash[1]);
    }

    private void addTypeAttrInfo(@NotNull StringBuilder buffer, @NotNull TypeContext.ClassType cls) {
        for (AttrWithOffset attrWithOffset : collectAttrs(cls)) {
            final var member = attrWithOffset.attr();
            final var hash = getHashString(getNestedTypeId(member.type()));
            final var category = Objects.requireNonNullElse(member.category(), "(none)");
            final var name = member.name();
            final var flags = member.flags() & 0xDEB;

            buffer.append("Attr: %s %s %s %d\n".formatted(hash, category, name, flags));
        }
    }

    private void addTypeBaseInfo(@NotNull StringBuilder buffer, @NotNull TypeContext.Type type, int indent) {
        buffer.append("  ".repeat(indent));

        if (type instanceof TypeContext.ClassType cls) {
            buffer.append("%s %X %X\n".formatted(getFullTypeName(type), cls.version(), findFlagsByMask(cls, 0xFFF)));

            for (TypeContext.ClassBase base : cls.bases()) {
                addTypeBaseInfo(buffer, base.type().get(), indent + 1);
            }
        } else {
            buffer.append("%s 0 0\n".formatted(getFullTypeName(type)));
        }
    }

    private int findFlagsByMask(@NotNull TypeContext.ClassType cls, int mask) {
        final int flags = cls.flags();

        if ((flags & mask) > 0) {
            return flags;
        }

        for (TypeContext.ClassBase base : cls.bases()) {
            final int flag = findFlagsByMask(base.type().get(), mask);

            if (flag > 0) {
                return flag;
            }
        }

        return 0;
    }

    @NotNull
    private String getFullTypeName(@NotNull TypeContext.Type type) {
        if (type instanceof TypeContext.TemplateType templateType) {
            return templateType.type().name() + '_' + getFullTypeName(templateType.argument().get());
        } else {
            return type.name();
        }
    }

    @NotNull
    private static String getHashString(@NotNull TypeId id) {
        final byte[] buf = new byte[32];
        IOUtils.toHexDigits(id.low(), buf, 0, ByteOrder.LITTLE_ENDIAN);
        IOUtils.toHexDigits(id.high(), buf, 16, ByteOrder.LITTLE_ENDIAN);
        return new String(buf, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
    }

    @NotNull
    private static List<AttrWithOffset> collectAttrs(@NotNull TypeContext.ClassType type) {
        final List<AttrWithOffset> fields = new ArrayList<>();
        collectAttrs(type, fields, 0);
        filterAttrs(fields);
        sortAttrs(fields);
        return fields;
    }

    private static void collectAttrs(@NotNull TypeContext.ClassType type, @NotNull List<AttrWithOffset> result, int offset) {
        for (TypeContext.ClassBase base : type.bases()) {
            collectAttrs(base.type().get(), result, base.offset() + offset);
        }
        for (TypeContext.ClassAttr attr : type.attrs()) {
            result.add(new AttrWithOffset(attr, attr.offset() + offset));
        }
    }

    private static void filterAttrs(@NotNull List<AttrWithOffset> attrs) {
        attrs.removeIf(attr -> (attr.attr().flags() & 2) != 0);
    }

    private static void sortAttrs(@NotNull List<AttrWithOffset> attrs) {
        RTTIUtils.quickSort(attrs, Comparator.comparingInt(AttrWithOffset::offset));
    }

    private record AttrWithOffset(@NotNull TypeContext.ClassAttr attr, int offset) {}
}
