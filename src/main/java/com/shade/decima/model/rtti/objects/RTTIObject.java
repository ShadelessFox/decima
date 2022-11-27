package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.util.NotNull;

import java.util.Map;

public record RTTIObject(@NotNull RTTITypeClass type, @NotNull Map<RTTITypeClass.Member, Object> members) {
    @NotNull
    public RTTITypeClass.Member getMember(@NotNull String name) {
        for (RTTITypeClass.Member member : members.keySet()) {
            if (member.name().equals(name)) {
                return member;
            }
        }

        return type.getMember(name);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T get(@NotNull RTTITypeClass.Member member) {
        if (type.isInstanceOf(member.parent())) {
            return (T) members.get(member);
        } else {
            throw new IllegalArgumentException("Invalid instance object");
        }
    }

    @NotNull
    public <T> T get(@NotNull String name) {
        return get(getMember(name));
    }

    @NotNull
    public RTTIObject obj(@NotNull String name) {
        return get(name);
    }

    @NotNull
    public String str(@NotNull String name) {
        return get(name).toString();
    }

    @NotNull
    public RTTIReference ref(@NotNull String name) {
        return get(name);
    }

    public byte i8(@NotNull String name) {
        return get(name);
    }

    public short i16(@NotNull String name) {
        return get(name);
    }

    public int i32(@NotNull String name) {
        return get(name);
    }

    public long i64(@NotNull String name) {
        return get(name);
    }

    public float f32(@NotNull String name) {
        return get(name);
    }

    public double f64(@NotNull String name) {
        return get(name);
    }

    public boolean bool(@NotNull String name) {
        return get(name);
    }

    public void set(@NotNull RTTITypeClass.Member member, @NotNull Object value) {
        if (type.isInstanceOf(member.parent())) {
            members.put(member, value);
        } else {
            throw new IllegalArgumentException("Invalid instance object");
        }
    }

    public void set(@NotNull String name, @NotNull Object value) {
        set(getMember(name), value);
    }

    public void define(@NotNull String name, @NotNull RTTIType<?> type, @NotNull Object value) {
        for (RTTITypeClass.Member member : members.keySet()) {
            if (member.name().equals(name)) {
                throw new IllegalArgumentException("Duplicate member " + name);
            }
        }

        set(new RTTITypeClass.Member(this.type, type, name, "", 0, 0), value);
    }
}
