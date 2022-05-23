package com.shade.decima.ui;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

public final class UIUtils {
    private static final Pattern TAG_PATTERN = Pattern.compile("<.*?>");

    private UIUtils() {
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
        return getParentNode(node, NavigatorProjectNode.class).getProject();
    }

    @NotNull
    public static Packfile getPackfile(@NotNull NavigatorNode node) {
        return getParentNode(node, NavigatorPackfileNode.class).getPackfile();
    }

    @NotNull
    public static <T extends NavigatorNode> T getParentNode(@NotNull NavigatorNode node, @NotNull Class<T> clazz) {
        for (NavigatorNode current = node; current != null; current = current.getParent()) {
            if (clazz.isInstance(current)) {
                return clazz.cast(current);
            }
        }

        throw new IllegalArgumentException("Can't find parent node of type " + clazz);
    }

    public static void minimizePanel(@NotNull JSplitPane pane, boolean topOrLeft) {
        try {
            final Field field = BasicSplitPaneUI.class.getDeclaredField("keepHidden");
            field.setAccessible(true);
            field.set(pane.getUI(), true);
        } catch (Exception ignored) {
            return;
        }

        pane.setLastDividerLocation(pane.getDividerLocation());
        pane.setDividerLocation(topOrLeft ? 0.0 : 1.0);
    }

    public static void delegateKey(@NotNull JComponent source, int sourceKeyCode, @NotNull JComponent target, @NotNull String targetActionKey) {
        delegateAction(source, KeyStroke.getKeyStroke(sourceKeyCode, 0), target, targetActionKey);
    }

    public static void delegateAction(@NotNull JComponent source, @NotNull JComponent target, @NotNull String targetActionKey, int targetCondition) {
        final InputMap inputMap = target.getInputMap(targetCondition);

        for (KeyStroke keyStroke : inputMap.allKeys()) {
            if (targetActionKey.equals(inputMap.get(keyStroke))) {
                delegateAction(source, keyStroke, target, targetActionKey);
            }
        }
    }

    private static void delegateAction(@NotNull JComponent source, @NotNull KeyStroke sourceKeyStroke, @NotNull JComponent target, @NotNull String targetActionKey) {
        final String sourceActionKey = "delegate-" + targetActionKey;

        source.getInputMap().put(sourceKeyStroke, sourceActionKey);
        source.getActionMap().put(sourceActionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Action action = target.getActionMap().get(targetActionKey);
                if (action != null) {
                    action.actionPerformed(new ActionEvent(target, e.getID(), targetActionKey, e.getWhen(), e.getModifiers()));
                }
            }
        });
    }

    public static record Mnemonic(@NotNull String text, int key, int index) {
    }
}
