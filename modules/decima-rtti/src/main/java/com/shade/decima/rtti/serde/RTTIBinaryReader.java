package com.shade.decima.rtti.serde;

import com.shade.util.NotNull;

import java.util.List;

public interface RTTIBinaryReader {
    @NotNull
    List<Object> read();
}
