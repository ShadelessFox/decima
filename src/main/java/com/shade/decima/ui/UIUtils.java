package com.shade.decima.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.SystemInfo;
import com.formdev.flatlaf.util.UIScale;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.controls.validation.InputValidator;
import com.shade.decima.ui.controls.validation.Validation;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;

public final class UIUtils {
    private UIUtils() {
    }

    @NotNull
    public static Color getInactiveTextColor() {
        return UIManager.getColor("Label.disabledForeground");
    }

    public static float getSmallerFontSize() {
        final Font font = UIManager.getFont("Label.font");
        return Math.max(font.getSize() - UIScale.scale(2f), UIScale.scale(11f));
    }

    public static void setRenderingHints(@NotNull Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, UIManager.get(RenderingHints.KEY_TEXT_ANTIALIASING));
        g.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, UIManager.get(RenderingHints.KEY_TEXT_LCD_CONTRAST));
    }

    public static void removeFrom(@NotNull Rectangle rect, @Nullable Insets insets) {
        if (insets != null) {
            rect.x += insets.left;
            rect.y += insets.top;
            rect.width -= insets.left + insets.right;
            rect.height -= insets.top + insets.bottom;
        }
    }

    @Nullable
    public static Mnemonic extractMnemonic(@NotNull String name) {
        final int index = name.indexOf('&');
        if (index >= 0 && name.length() > index + 1 && name.charAt(index + 1) != '&') {
            return new Mnemonic(name.substring(0, index) + name.substring(index + 1), name.charAt(index + 1), index);
        } else {
            return null;
        }
    }

    public static void installInputValidator(@NotNull JComponent component, @NotNull InputValidator validator, @Nullable PropertyChangeListener validationListener) {
        component.setInputVerifier(validator);

        if (validationListener != null) {
            validator.getPropertyChangeSupport().addPropertyChangeListener(InputValidator.PROPERTY_VALIDATION, validationListener);
        }
    }

    public static boolean isValid(@NotNull JComponent component) {
        final InputVerifier verifier = component.getInputVerifier();
        if (verifier instanceof InputValidator validator) {
            final Validation validation = validator.getLastValidation();
            return validation == null || validation.isOK();
        }
        return true;
    }

    public static void addOpenFileAction(@NotNull JTextField component, @NotNull String title, @Nullable FileFilter filter) {
        final JToolBar toolBar = new JToolBar();

        toolBar.add(new AbstractAction(null, UIManager.getIcon("Tree.openIcon")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();

                chooser.setDialogTitle(title);
                chooser.setFileFilter(filter);
                chooser.setAcceptAllFileFilterUsed(false);

                if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
                    component.setText(chooser.getSelectedFile().toString());
                }
            }
        });

        component.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolBar);
    }

    public static void addOpenDirectoryAction(@NotNull JTextField component, @NotNull String title) {
        final JToolBar toolBar = new JToolBar();

        toolBar.add(new AbstractAction(null, UIManager.getIcon("Tree.openIcon")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();

                chooser.setDialogTitle(title);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
                    component.setText(chooser.getSelectedFile().toString());
                }
            }
        });

        component.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolBar);
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

    public static void browseFileDirectory(@NotNull Path path) {
        final Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR) && SystemInfo.isWindows) {
            try {
                Runtime.getRuntime().exec("explorer /E,/select=" + path);
                return;
            } catch (IOException ignored) {
            }
        }

        desktop.browseFileDirectory(path.toFile());
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
