package com.shade.decima.model.rtti.messages.shared;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.RTTIField;

public abstract class VertexStream {

    @RTTIField(type = @Type(name = "MurmurHashValue"))
    public RTTIObject hash;
    @RTTIField(type = @Type(name = "Array<uint8>"))
    public byte[] data;
    @RTTIField(type = @Type(name = "uint32"))
    public int flags;
    @RTTIField(type = @Type(name = "uint32"))
    public int stride;

    public abstract RTTIObject[] elements();
}
