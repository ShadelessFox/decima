package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.PathElementUUID;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

public class CoreNodeEntry extends CoreNodeObject {
    private final int index;

    public CoreNodeEntry(@NotNull CoreNodeBinary parent, @NotNull RTTIObject object, int index) {
        super(parent, object.type(), object, RTTITypeRegistry.getFullTypeName(object.type()), new PathElementUUID(object.get("ObjectUUID")));
        this.index = index;
    }

    @NotNull
    public RTTIObject getObjectUUID() {
        return getObject().get("ObjectUUID");
    }

    public int getIndex() {
        return index;
    }

    @NotNull
    @Override
    public RTTIObject getObject() {
        return (RTTIObject) super.getObject();
    }
}
