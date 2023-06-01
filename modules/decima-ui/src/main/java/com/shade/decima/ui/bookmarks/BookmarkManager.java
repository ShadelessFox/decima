package com.shade.decima.ui.bookmarks;

import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.messages.Topic;
import com.shade.util.NotNull;

public interface BookmarkManager {
    Topic<BookmarkListener> BOOKMARKS = Topic.create("bookmarks", BookmarkListener.class);

    @NotNull
    static BookmarkManager getInstance() {
        return ApplicationManager.getApplication().getService(BookmarkManager.class);
    }

    void addBookmark(@NotNull Bookmark bookmark);

    void removeBookmark(@NotNull Bookmark bookmark);

    boolean hasBookmark(@NotNull Bookmark.Location path);

    @NotNull
    Bookmark[] getBookmarks();
}
