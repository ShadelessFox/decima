package com.shade.decima.ui;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.navigator.NavigatorNode;

import javax.swing.tree.TreePath;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;

public final class UIUtils {
    private static final Pattern TAG_PATTERN = Pattern.compile("<.*?>");

    private UIUtils() {
    }

    @NotNull
    public static TreePath getPath(@NotNull NavigatorNode node) {
        final Deque<NavigatorNode> nodes = new ArrayDeque<>();
        for (NavigatorNode current = node; current != null; current = current.getParent()) {
            nodes.offerFirst(current);
        }
        return new TreePath(nodes.toArray());
    }

    @NotNull
    public static String escapeHtmlEntities(@NotNull String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @NotNull
    public static String unescapeHtmlEntities(@NotNull String text) {
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    @NotNull
    public static String removeHtmlTags(@NotNull String text) {
        return TAG_PATTERN.matcher(text).replaceAll("");
    }
}
