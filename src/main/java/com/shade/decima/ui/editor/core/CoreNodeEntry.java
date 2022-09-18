package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.PathElementUUID;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

public class CoreNodeEntry extends CoreNodeObject {
    private final RTTIObject objectUUID;

    public CoreNodeEntry(@NotNull CoreNodeBinary parent, @NotNull RTTIObject object) {
        super(parent, object.getType(), object, RTTITypeRegistry.getFullTypeName(object.getType()), new PathElementUUID(object.get("ObjectUUID")));
        this.objectUUID = object.get("ObjectUUID");
    }

    @NotNull
    public RTTIObject getObjectUUID() {
        return objectUUID;
    }

    @NotNull
    @Override
    public RTTIObject getObject() {
        return (RTTIObject) super.getObject();
    }
}
