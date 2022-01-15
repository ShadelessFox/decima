package com.shade.decima.rtti.objects;

import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.util.NotNull;

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
    public <T> T getMemberValue(@NotNull RTTITypeClass.Member member) {
        return (T) Objects.requireNonNull(members.get(member));
    }

    @NotNull
    public <T> T getMemberValue(@NotNull String name) {
        return getMemberValue(getMember(name));
    }

    @NotNull
    public RTTITypeClass.Member getMember(@NotNull String name) {
        for (RTTITypeClass.Member member : members.keySet()) {
            if (member.name().equals(name)) {
                return member;
            }
        }
        throw new IllegalArgumentException("Object of type " + type.getName() + " does not have a member called '" + name + "'");
    }
}
