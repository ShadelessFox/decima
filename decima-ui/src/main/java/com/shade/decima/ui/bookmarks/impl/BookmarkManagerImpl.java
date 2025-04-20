package com.shade.decima.ui.bookmarks.impl;

import com.shade.decima.model.app.ProjectChangeListener;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.ui.bookmarks.Bookmark;
import com.shade.decima.ui.bookmarks.BookmarkManager;
import com.shade.platform.model.Service;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.messages.MessageBusConnection;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service(BookmarkManager.class)
@Persistent("BookmarkManager")
public class BookmarkManagerImpl implements BookmarkManager, PersistableComponent<Bookmark[]> {
    private final Map<Bookmark.Location, Bookmark> bookmarks = Collections.synchronizedMap(new LinkedHashMap<>());

    public BookmarkManagerImpl() {
        final MessageBusConnection connection = MessageBus.getInstance().connect();
        connection.subscribe(ProjectManager.PROJECTS, new ProjectChangeListener() {
            @Override
            public void projectRemoved(@NotNull ProjectContainer container) {
                final Bookmark[] bookmarksToRemove = bookmarks.values().stream()
                    .filter(bookmark -> bookmark.location().project().equals(container.getId().toString()))
                    .toArray(Bookmark[]::new);

                for (Bookmark bookmark : bookmarksToRemove) {
                    removeBookmark(bookmark);
                }
            }
        });
    }

    @Override
    public void addBookmark(@NotNull Bookmark bookmark) {
        if (bookmarks.putIfAbsent(bookmark.location(), bookmark) == null) {
            MessageBus.getInstance().publisher(BOOKMARKS).bookmarkAdded(bookmark);
        }
    }

    @Override
    public void removeBookmark(@NotNull Bookmark bookmark) {
        if (bookmarks.remove(bookmark.location()) != null) {
            MessageBus.getInstance().publisher(BOOKMARKS).bookmarkRemoved(bookmark);
        }
    }

    @Override
    public boolean hasBookmark(@NotNull Bookmark.Location path) {
        return bookmarks.containsKey(path);
    }

    @NotNull
    @Override
    public Bookmark[] getBookmarks() {
        return bookmarks.values().toArray(Bookmark[]::new);
    }

    @Nullable
    @Override
    public Bookmark[] getState() {
        return bookmarks.values().toArray(Bookmark[]::new);
    }

    @Override
    public void loadState(@NotNull Bookmark[] state) {
        for (Bookmark bookmark : state) {
            bookmarks.put(bookmark.location(), bookmark);
        }
    }
}
