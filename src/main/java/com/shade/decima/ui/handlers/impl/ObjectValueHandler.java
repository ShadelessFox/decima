package com.shade.decima.ui.handlers.impl;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.ui.handlers.ValueCollectionHandler;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class ObjectValueHandler implements ValueCollectionHandler<RTTIObject, RTTITypeClass.Member> {
    public static final ObjectValueHandler INSTANCE = new ObjectValueHandler();

    private ObjectValueHandler() {
    }

    @Nullable
    @Override
    public String getInlineValue(@NotNull RTTIType<?> type, @NotNull Object value) {
        return null;
    }

    @NotNull
    @Override
    public Collection<RTTITypeClass.Member> getChildren(@NotNull RTTIType<?> type, @NotNull RTTIObject object) {
        final ArrayList<RTTITypeClass.Member> members = new ArrayList<>(object.getMembers().keySet());
        members.sort(Comparator
            .comparing((RTTITypeClass.Member x) -> x.type().getKind() == RTTIType.Kind.CLASS || x.type().getKind() == RTTIType.Kind.CONTAINER)
            .reversed());
        return members;
    }

    @NotNull
    @Override
    public String getChildName(@NotNull RTTIType<?> type, @NotNull RTTIObject object, @NotNull RTTITypeClass.Member member) {
        return member.name();
    }

    @NotNull
    @Override
    public Object getChildValue(@NotNull RTTIType<?> type, @NotNull RTTIObject object, @NotNull RTTITypeClass.Member member) {
        return object.getMemberValue(member);
    }

    @NotNull
    @Override
    public RTTIType<?> getChildType(@NotNull RTTIType<?> type, @NotNull RTTIObject object, @NotNull RTTITypeClass.Member member) {
        return member.type();
    }
}
