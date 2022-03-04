package com.shade.decima.ui;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.decima.ui.resources.Project;

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

    @Nullable
    public static Mnemonic extractMnemonic(@NotNull String name) {
        final int index = name.indexOf('&');
        if (index >= 0 && name.length() > index + 1 && name.charAt(index + 1) != '&') {
            return new Mnemonic(name.substring(0, index) + name.substring(index + 1), name.charAt(index), index);
        }
        return null;
    }

    @NotNull
    public static Project getProject(@NotNull NavigatorNode node) {
        final NavigatorProjectNode project = getParentNode(node, NavigatorProjectNode.class);
        if (project == null) {
            throw new IllegalArgumentException("Incorrect node hierarchy");
        }
        return project.getProject();
    }

    @Nullable
    public static <T extends NavigatorNode> T getParentNode(@NotNull NavigatorNode node, @NotNull Class<T> clazz) {
        for (NavigatorNode current = node; current != null; current = current.getParent()) {
            if (clazz.isInstance(current)) {
                return clazz.cast(current);
            }
        }
        return null;
    }

    public static record Mnemonic(@NotNull String text, int key, int index) {
    }
}
