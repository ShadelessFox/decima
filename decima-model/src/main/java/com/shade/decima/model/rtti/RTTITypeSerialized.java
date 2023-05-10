package com.shade.decima.model.rtti;

import com.shade.decima.model.base.CoreBinary;
import com.shade.util.Nullable;

/**
 * A serializable type that has a unique identifier.
 * <p>
 * Such identifiers are used to identify type of entries in core files.
 *
 * @see CoreBinary
 */
public interface RTTITypeSerialized {
    @Nullable
    TypeId getTypeId();

    record TypeId(long low, long high) {}
}
