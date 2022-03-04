package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.rtti.types.RTTITypeClass;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.RTTIUtils;

import java.util.Map;
import java.util.Objects;

public final class RTTIObject {
    private final RTTITypeClass type;
    private final Map<RTTITypeClass.Member, Object> members;

    public RTTIObject(@NotNull RTTITypeClass type, @NotNull Map<RTTITypeClass.Member, Object> members) {
        this.type = type;
        this.members = members;
    }

    @NotNull
    public RTTITypeClass getType() {
        return type;
    }

    @NotNull
    public Map<RTTITypeClass.Member, Object> getMembers() {
        return members;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T get(@NotNull RTTITypeClass.Member member) {
        return (T) Objects.requireNonNull(members.get(member));
    }

    @NotNull
    public <T> T get(@NotNull String name) {
        return get(type.getMember(name));
    }

    public void set(@NotNull RTTITypeClass.Member member, @NotNull Object value) {
        members.put(member, value);
    }

    public void set(@NotNull String name, @NotNull Object value) {
        set(name, value, false);
    }

    public void set(@NotNull String name, @NotNull Object value, boolean create) {
        RTTITypeClass.Member member = null;

        for (RTTITypeClass.Member m : type.getMembers()) {
            if (m.name().equals(name)) {
                member = m;
                break;
            }
        }

        if (member == null && create) {
            member = new RTTITypeClass.Member(type, RTTIUtils.getObjectType(value), name, "", 0, 0);
        }

        if (member == null) {
            throw new IllegalArgumentException("Type " + type.getName() + " has no member called '" + name + "'");
        }

        set(member, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RTTIObject that = (RTTIObject) o;
        return type.equals(that.type) && members.equals(that.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, members);
    }
}
