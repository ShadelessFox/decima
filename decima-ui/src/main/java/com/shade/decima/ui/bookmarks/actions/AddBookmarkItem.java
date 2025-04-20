package com.shade.decima.ui.bookmarks.actions;

import com.shade.decima.ui.bookmarks.Bookmark;
import com.shade.decima.ui.bookmarks.BookmarkManager;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Add &Bookmark", group = CTX_MENU_NAVIGATOR_GROUP_BOOKMARKS, order = 1000)
public class AddBookmarkItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final NavigatorFileNode node = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final Bookmark bookmark = new Bookmark(Bookmark.Location.of(node), node.getLabel());
        BookmarkManager.getInstance().addBookmark(bookmark);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorFileNode node
            && !BookmarkManager.getInstance().hasBookmark(Bookmark.Location.of(node));
    }
}
