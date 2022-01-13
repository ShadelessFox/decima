package com.shade.decima.rtti.registry;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.RTTITypeContainer;
import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.rtti.types.RTTITypeEnum;
import com.shade.decima.rtti.types.RTTITypeEnumFlags;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.hash.MurmurHash3;

import java.util.HashMap;
import java.util.Map;

public class RTTITypeDumper {
    private final Map<RTTIType<?>, long[]> cache = new HashMap<>();
    private final Map<RTTIType<?>, long[]> nestedCache = new HashMap<>();

    public RTTITypeDumper() {
    }

    public long[] getTypeId(@NotNull RTTIType<?> type) {
        return getTypeId(type, 0);
    }

    public long[] getTypeId(@NotNull RTTIType<?> type, int indent) {
        final StringBuilder sb = new StringBuilder();
        sb.append("RTTIBinaryVersion: 2, Type: ").append(sanitizeName(type));

        sb.append('\n');

        if (type instanceof RTTITypeClass cls) {
            sb.append("  ".repeat(indent));
            addTypeAttrInfo(sb, cls);
        }

        addTypeBaseInfo(sb, type, indent);

        if (type instanceof RTTITypeEnum enumeration) {
            sb.append("  ".repeat(indent));
            sb.append("Enum-Size: %d\n".formatted(enumeration.getSize()));

            for (RTTITypeEnum.Constant constant : enumeration.getConstants()) {
                sb.append("  ".repeat(indent));
                sb.append("Enumeration-Value: %s %s\n".formatted(constant.value(), constant.name()));
            }
        }

        if (type instanceof RTTITypeEnumFlags enumeration) {
            sb.append("  ".repeat(indent));

            for (RTTITypeEnumFlags.Constant constant : enumeration.getConstants()) {
                sb.append("  ".repeat(indent));
                sb.append("Enumeration-Value: %s %s\n".formatted(constant.value(), constant.name()));
            }
        }

        if (type instanceof RTTITypeContainer<?> container && type.getKind() == RTTIType.Kind.CONTAINER) {
            sb.append("  ".repeat(indent));
            sb.append("Contained-Type: %s\n".formatted(hashToString(getNestedTypeId(container.getContainedType()))));
        }

        if (cache.containsKey(type)) {
            return cache.get(type);
        }

        final long[] hash = MurmurHash3.mmh3(sb.toString().getBytes());
//        System.out.println(hashToString(hash) + " = " + sb);
        cache.put(type, hash);
        return hash;
    }

    private long[] getNestedTypeId(@NotNull RTTIType<?> type) {
        if (!nestedCache.containsKey(type)) {
            nestedCache.put(type, MurmurHash3.mmh3(("RTTIBinaryVersion: 2, Type: " + sanitizeName(type)).getBytes()));
            nestedCache.put(type, getTypeId(type));
        }
        return nestedCache.get(type);
    }

    private void addTypeAttrInfo(@NotNull StringBuilder buffer, @NotNull RTTITypeClass cls) {
        for (RTTITypeClass.MemberInfo info : cls.getOrderedMembers()) {
            final RTTITypeClass.Member member = info.member();

            if (member.isSaveState()) {
                continue;
            }

            final var hash = hashToString(getTypeId(member.type()));
            final var category = member.category();
            final var name = member.name();
            final var flags = member.flags() & 0xDEB;

            buffer.append("Attr: %s %s %s %d\n".formatted(hash, category == null ? "(none)" : category, name, flags));
        }
    }

    private void addTypeBaseInfo(@NotNull StringBuilder buffer, @NotNull RTTIType<?> type, int indent) {
        buffer.append("  ".repeat(indent));

        if (type instanceof RTTITypeClass cls) {
            buffer.append("%s %X %X\n".formatted(sanitizeName(type), cls.getUnknownFlags(), collectFlags(cls, 0xFFF)));

            for (RTTITypeClass.Base base : cls.getBases()) {
                addTypeBaseInfo(buffer, base.type(), indent + 1);
            }
        } else {
            buffer.append("%s 0 0\n".formatted(sanitizeName(type)));
        }
    }

    private int collectFlags(@NotNull RTTITypeClass cls, int mask) {
        final int flags = cls.getFlags();

        if ((flags & mask) > 0) {
            return flags;
        }

        for (RTTITypeClass.Base base : cls.getBases()) {
            final int flag = collectFlags(base.type(), mask);

            if (flag > 0) {
                return flag;
            }
        }

        return 0;
    }

    @NotNull
    private String sanitizeName(@NotNull RTTIType<?> type) {
        return sanitizeName(RTTITypeRegistry.getFullTypeName(type));
    }

    @NotNull
    private static String sanitizeName(@NotNull String name) {
        return name.replace('<', '_').replace('>', '_').replace("CPtr", "cptr").replaceAll("_+$", "");
    }

    @NotNull
    private static String hashToString(@NotNull long[] hash) {
        final StringBuilder sb = new StringBuilder(32);
        for (byte b : IOUtils.toByteArray(hash)) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
