package com.shade.decima.ui.bookmarks.actions;

import com.shade.decima.ui.bookmarks.Bookmark;
import com.shade.decima.ui.bookmarks.BookmarkManager;
import com.shade.decima.ui.bookmarks.BookmarkView;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_BOOKMARKS_ID, name = "&Delete", keystroke = "DELETE", group = CTX_MENU_BOOKMARKS_GROUP_GENERAL, order = 2000)
public class DeleteBookmarkItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Bookmark bookmark = ctx.getData(BookmarkView.BOOKMARK_KEY);
        BookmarkManager.getInstance().removeBookmark(bookmark);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(BookmarkView.BOOKMARK_KEY) != null;
    }
}
