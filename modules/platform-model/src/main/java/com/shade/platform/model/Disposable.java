package com.shade.platform.model;

import com.shade.util.Nullable;

public interface Disposable {
    /**
     * This function provides a convenient way to dispose a nullable {@link Disposable} without redundant null check.
     */
    static void dispose(@Nullable Disposable disposable) {
        if (disposable != null) {
            disposable.dispose();
        }
    }

    void dispose();
}
