package com.shade.decima.ui.handlers.impl;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.types.RTTITypeClass;
import com.shade.decima.ui.handlers.ValueCollectionHandler;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.util.Collection;

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
        return object.getMembers().keySet();
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
