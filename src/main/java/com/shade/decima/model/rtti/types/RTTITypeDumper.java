package com.shade.decima.model.rtti.types;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.RTTITypeSerialized.TypeId;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RTTITypeDumper {
    private final Map<RTTIType<?>, TypeId> cache = new HashMap<>();
    private final Set<RTTIType<?>> pending = new HashSet<>();

    @NotNull
    public String getTypeString(@NotNull RTTIType<?> type) {
        final StringBuilder sb = new StringBuilder()
            .append("RTTIBinaryVersion: 2, Type: ").append(getFullTypeName(type)).append('\n');

        if (type instanceof RTTITypeClass cls) {
            addTypeAttrInfo(sb, cls);
        }

        addTypeBaseInfo(sb, type, 0);

        if (type instanceof RTTITypeEnum enumeration) {
            sb.append("Enum-Size: %d\n".formatted(enumeration.getSize()));

            for (RTTITypeEnum.Constant constant : enumeration.getConstants()) {
                sb.append("Enumeration-Value: %s %s\n".formatted(constant.value(), constant.name()));
            }
        }

        if (type instanceof RTTITypeEnumFlags enumeration) {
            for (RTTITypeEnumFlags.Constant constant : enumeration.getConstants()) {
                sb.append("Enumeration-Value: %s %s\n".formatted(constant.value(), constant.name()));
            }
        }

        if (type instanceof RTTITypeContainer<?, ?> container) {
            sb.append("Contained-Type: %s\n".formatted(getHashString(getNestedTypeId(container.getComponentType()))));
        }

        return sb.toString();
    }

    @NotNull
    public TypeId getTypeId(@NotNull RTTIType<?> type) {
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
    private TypeId getNestedTypeId(@NotNull RTTIType<?> type) {
        if (pending.contains(type)) {
            return getShortTypeId(type);
        } else {
            return getTypeId(type);
        }
    }

    @NotNull
    private TypeId getShortTypeId(@NotNull RTTIType<?> type) {
        return getTypeId("RTTIBinaryVersion: 2, Type: " + getFullTypeName(type));
    }

    @NotNull
    private TypeId getTypeId(@NotNull String string) {
        final long[] hash = MurmurHash3.mmh3(string.getBytes(StandardCharsets.UTF_8));
        return new TypeId(hash[0], hash[1]);
    }

    private void addTypeAttrInfo(@NotNull StringBuilder buffer, @NotNull RTTITypeClass cls) {
        for (RTTITypeClass.FieldWithOffset info : cls.getOrderedMembers()) {
            final var member = info.field();

            if (member.isNonHashable()) {
                continue;
            }

            final var hash = getHashString(getNestedTypeId(member.type()));
            final var category = Objects.requireNonNullElse(member.category(), "(none)");
            final var name = member.name();
            final var flags = member.flags() & 0xDEB;

            buffer.append("Attr: %s %s %s %d\n".formatted(hash, category, name, flags));
        }
    }

    private void addTypeBaseInfo(@NotNull StringBuilder buffer, @NotNull RTTIType<?> type, int indent) {
        buffer.append("  ".repeat(indent));

        if (type instanceof RTTITypeClass cls) {
            buffer.append("%s %X %X\n".formatted(getFullTypeName(type), cls.getVersion(), findFlagsByMask(cls, 0xFFF)));

            for (RTTITypeClass.MySuperclass superclass : cls.getSuperclasses()) {
                addTypeBaseInfo(buffer, superclass.type(), indent + 1);
            }
        } else {
            buffer.append("%s 0 0\n".formatted(getFullTypeName(type)));
        }
    }

    private int findFlagsByMask(@NotNull RTTITypeClass cls, int mask) {
        final int flags = cls.getFlags();

        if ((flags & mask) > 0) {
            return flags;
        }

        for (RTTITypeClass.MySuperclass superclass : cls.getSuperclasses()) {
            final int flag = findFlagsByMask(superclass.type(), mask);

            if (flag > 0) {
                return flag;
            }
        }

        return 0;
    }

    @NotNull
    private String getFullTypeName(@NotNull RTTIType<?> type) {
        if (type instanceof RTTITypeParameterized<?, ?> parameterized) {
            return type.getTypeName() + '_' + getFullTypeName(parameterized.getComponentType());
        } else {
            return type.getTypeName();
        }
    }

    @NotNull
    private static String getHashString(@NotNull TypeId id) {
        final byte[] buf = new byte[32];
        IOUtils.toHexDigits(id.low(), buf, 0, ByteOrder.LITTLE_ENDIAN);
        IOUtils.toHexDigits(id.high(), buf, 16, ByteOrder.LITTLE_ENDIAN);
        return new String(buf, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
    }
}
