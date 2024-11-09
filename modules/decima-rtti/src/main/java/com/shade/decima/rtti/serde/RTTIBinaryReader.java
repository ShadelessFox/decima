package com.shade.decima.rtti.serde;

import com.shade.util.NotNull;

import java.io.IOException;
import java.util.List;

public interface RTTIBinaryReader {
    @NotNull
    List<Object> read() throws IOException;
}
