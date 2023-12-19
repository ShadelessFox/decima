package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;

public record BufferView(@NotNull Buffer buffer, int offset, int length) {
}
