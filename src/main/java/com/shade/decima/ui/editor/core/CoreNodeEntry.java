package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.PathElementUUID;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.util.NotNull;

public class CoreNodeEntry extends CoreNodeObject {
    public CoreNodeEntry(@NotNull CoreNodeBinary parent, @NotNull RTTIObject object) {
        super(parent, object.getType(), object, RTTITypeRegistry.getFullTypeName(object.getType()), new PathElementUUID(object.get("ObjectUUID")));
    }

    @NotNull
    public RTTIObject getObjectUUID() {
        return getObject().get("ObjectUUID");
    }

    @NotNull
    @Override
    public RTTIObject getObject() {
        return (RTTIObject) super.getObject();
    }
}
