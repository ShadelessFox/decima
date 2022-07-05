package com.shade.decima.model.rtti.registry;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.RTTITypeContainer;
import com.shade.decima.model.rtti.RTTITypeParameterized;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.RTTITypeEnumFlags;
import com.shade.decima.model.util.IOUtils;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.hash.MurmurHash3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RTTITypeDumper {
    private final Map<RTTIType<?>, long[]> cache = new HashMap<>();
    private final Map<RTTIType<?>, long[]> nestedCache = new HashMap<>();

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
            sb.append("Contained-Type: %s\n".formatted(getHashString(getNestedTypeId(container.getArgumentType()))));
        }

        return sb.toString();
    }

    @NotNull
    public long[] getTypeId(@NotNull RTTIType<?> type) {
        final String string = getTypeString(type);

        if (cache.containsKey(type)) {
            return cache.get(type);
        }

        final long[] hash = MurmurHash3.mmh3(string.getBytes());
        cache.put(type, hash);
        return hash;
    }

    private long[] getNestedTypeId(@NotNull RTTIType<?> type) {
        if (!nestedCache.containsKey(type)) {
            nestedCache.put(type, MurmurHash3.mmh3(("RTTIBinaryVersion: 2, Type: " + getFullTypeName(type)).getBytes()));
            nestedCache.put(type, getTypeId(type));
        }
        return nestedCache.get(type);
    }

    private void addTypeAttrInfo(@NotNull StringBuilder buffer, @NotNull RTTITypeClass cls) {
        for (RTTITypeClass.MemberInfo info : cls.getOrderedMembers()) {
            final var member = info.member();
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
            buffer.append("%s %X %X\n".formatted(getFullTypeName(type), cls.getFlags1(), findFlagsByMask(cls, 0xFFF)));

            for (RTTITypeClass.Base base : cls.getBases()) {
                addTypeBaseInfo(buffer, base.type(), indent + 1);
            }
        } else {
            buffer.append("%s 0 0\n".formatted(getFullTypeName(type)));
        }
    }

    private int findFlagsByMask(@NotNull RTTITypeClass cls, int mask) {
        final int flags = cls.getFlags2();

        if ((flags & mask) > 0) {
            return flags;
        }

        for (RTTITypeClass.Base base : cls.getBases()) {
            final int flag = findFlagsByMask(base.type(), mask);

            if (flag > 0) {
                return flag;
            }
        }

        return 0;
    }

    @NotNull
    private String getFullTypeName(@NotNull RTTIType<?> type) {
        if (type instanceof RTTITypeParameterized<?, ?> parameterized) {
            return type.getTypeName() + '_' + getFullTypeName(parameterized.getArgumentType());
        } else {
            return type.getTypeName();
        }
    }

    @NotNull
    private static String getHashString(@NotNull long[] hash) {
        final StringBuilder sb = new StringBuilder(32);
        for (byte b : IOUtils.toByteArray(hash)) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
