package com.shade.decima.game.hfw.rtti.data;

import com.shade.decima.rtti.Attr;

public interface StreamingDataSourceExtension {
    @Attr(name = "Locator", type = "uint64", position = 0, offset = 0)
    long locator();

    void locator(long locator);
}
