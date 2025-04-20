package com.shade.decima.ui.bookmarks.actions;

import com.shade.decima.ui.bookmarks.Bookmark;
import com.shade.decima.ui.bookmarks.BookmarkManager;
import com.shade.decima.ui.bookmarks.BookmarkView;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_BOOKMARKS_ID, name = "Re&name\u2026", keystroke = "ctrl ENTER", group = CTX_MENU_BOOKMARKS_GROUP_GENERAL, order = 1000)
public class RenameBookmarkItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Bookmark bookmark = ctx.getData(BookmarkView.BOOKMARK_KEY);
        final String name = JOptionPane.showInputDialog("Rename bookmark '%s' to:".formatted(bookmark.name()), bookmark.name());

        if (name != null && !name.isBlank()) {
            BookmarkManager.getInstance().removeBookmark(bookmark);
            BookmarkManager.getInstance().addBookmark(new Bookmark(bookmark.location(), name));
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(BookmarkView.BOOKMARK_KEY) != null;
    }
}
