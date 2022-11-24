package com.shade.decima.model.rtti;

import com.shade.util.Nullable;

/**
 * A serializable type that has a unique identifier.
 * <p>
 * Such identifiers are used to identify type of entries in core files.
 *
 * @see com.shade.decima.model.base.CoreBinary
 */
public abstract class RTTITypeSerialized<T_INSTANCE> extends RTTIType<T_INSTANCE> {
    @Nullable
    public abstract TypeId getTypeId();

    public record TypeId(long low, long high) {}
}
