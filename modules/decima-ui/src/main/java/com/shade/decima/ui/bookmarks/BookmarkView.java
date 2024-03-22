package com.shade.decima.ui.bookmarks;

import com.shade.decima.ui.bookmarks.impl.BookmarkNode;
import com.shade.decima.ui.bookmarks.impl.BookmarkNodeRoot;
import com.shade.decima.ui.bookmarks.impl.BookmarkTree;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.decima.ui.views.BaseView;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.messages.MessageBusConnection;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.platform.ui.util.UIUtils;
import com.shade.platform.ui.views.ViewRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

@ViewRegistration(id = BookmarkView.ID, label = "Bookmarks", icon = "Action.starIcon", keystroke = "alt 2")
public class BookmarkView extends BaseView<Tree> {
    public static final String ID = "bookmarks";
    public static final DataKey<Bookmark> BOOKMARK_KEY = new DataKey<>("bookmark", Bookmark.class);

    public BookmarkView() {
        final MessageBusConnection connection = MessageBus.getInstance().connect();
        connection.subscribe(BookmarkManager.BOOKMARKS, new BookmarkListener() {
            @Override
            public void bookmarkAdded(@NotNull Bookmark bookmark) {
                final TreeModel model = component.getModel();
                final BookmarkNodeRoot root = (BookmarkNodeRoot) model.getRoot();

                root.addChild(new BookmarkNode(root, bookmark), root.getChildCount());
                model.fireStructureChanged(root);
            }

            @Override
            public void bookmarkRemoved(@NotNull Bookmark bookmark) {
                component.getModel()
                    .findChild(new VoidProgressMonitor(), node -> ((BookmarkNode) node).getBookmark() == bookmark)
                    .thenAccept(node -> {
                        final TreeModel model = component.getModel();
                        final BookmarkNodeRoot root = (BookmarkNodeRoot) model.getRoot();

                        root.removeChild(root.getChildIndex(node));
                        model.fireStructureChanged(root);
                    });
            }
        });
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        final JScrollPane pane = UIUtils.createBorderlessScrollPane(super.createComponent());
        pane.setPreferredSize(new Dimension(250, 0));

        return pane;
    }

    @NotNull
    @Override
    protected Tree createComponentImpl() {
        final BookmarkTree tree = new BookmarkTree();

        MenuManager.getInstance().installContextMenu(tree, MenuConstants.CTX_MENU_BOOKMARKS_ID, key -> switch (key) {
            case "bookmark" -> tree.getLastSelectedPathComponent() instanceof BookmarkNode node ? node.getBookmark() : null;
            default -> null;
        });

        return tree;
    }

    @Override
    public void setFocus() {
        component.requestFocusInWindow();
    }

    @Override
    public boolean isFocused() {
        return component.hasFocus();
    }
}
