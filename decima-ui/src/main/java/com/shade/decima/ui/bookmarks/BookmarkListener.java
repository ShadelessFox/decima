package com.shade.decima.ui.bookmarks;

import com.shade.util.NotNull;

public interface BookmarkListener {
    default void bookmarkAdded(@NotNull Bookmark bookmark) {
        // do nothing by default
    }

    default void bookmarkRemoved(@NotNull Bookmark bookmark) {
        // do nothing by default
    }
}
