package com.shade.platform.ui.util;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.model.util.ThrowableSupplier;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.controls.validation.Validation;
import com.shade.platform.ui.dialogs.ExceptionDialog;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Objects;

public final class UIUtils {
    private UIUtils() {
    }

    @NotNull
    public static Font getDefaultFont() {
        Font font = UIManager.getFont("defaultFont");

        if (font == null) {
            font = UIManager.getFont("Label.font");
        }

        return font;
    }

    public static int getDefaultFontSize() {
        return getDefaultFont().getSize();
    }

    public static int getSmallerFontSize() {
        return UIManager.getFont("medium.font").getSize();
    }

    public static void setRenderingHints(@NotNull Graphics2D g) {
        final Object textAliasing = UIManager.get(RenderingHints.KEY_TEXT_ANTIALIASING);
        final Object lcdContrast = UIManager.get(RenderingHints.KEY_TEXT_LCD_CONTRAST);

        if (textAliasing != null) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAliasing);
        }

        if (lcdContrast != null) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, lcdContrast);
        }
    }

    public static void removeFrom(@NotNull Rectangle rect, @Nullable Insets insets) {
        if (insets != null) {
            rect.x += insets.left;
            rect.y += insets.top;
            rect.width -= insets.left + insets.right;
            rect.height -= insets.top + insets.bottom;
        }
    }

    public static void removeFrom(@NotNull Dimension dimension, @Nullable Insets insets) {
        if (insets != null) {
            dimension.width -= insets.left + insets.right;
            dimension.height -= insets.top + insets.bottom;
        }
    }

    public static float getScalingFactor(float width1, float height1, float width2, float height2) {
        final float rs = width1 / height1;
        final float ri = width2 / height2;

        if (rs > ri) {
            return height1 / height2;
        } else {
            return width1 / width2;
        }
    }

    @NotNull
    public static Color mix(@NotNull Color first, @NotNull Color second, float factor) {
        if (factor <= 0.0f) {
            return first;
        } else if (factor >= 1.0f) {
            return second;
        } else {
            final float inv = 1f - factor;
            final int r = (int) (first.getRed() * inv + second.getRed() * factor);
            final int g = (int) (first.getGreen() * inv + second.getGreen() * factor);
            final int b = (int) (first.getBlue() * inv + second.getBlue() * factor);
            return new Color(r, g, b);
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
        addOpenAction(component, e -> {
            final JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle(title);
            chooser.setFileFilter(filter);
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
                component.setText(chooser.getSelectedFile().toString());
            }
        });
    }

    public static void addOpenAction(@NotNull JTextField component, @NotNull ActionListener delegate) {
        addAction(component, UIManager.getIcon("Tree.openIcon"), delegate);
    }

    public static void addCopyAction(@NotNull JTextField component) {
        addAction(component, UIManager.getIcon("Action.copyIcon"), e -> {
            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            final StringSelection contents = new StringSelection(component.getText());
            clipboard.setContents(contents, contents);
        });
    }

    public static void addAction(@NotNull JTextField component, @NotNull Icon icon, @NotNull ActionListener delegate) {
        final AbstractAction action = new AbstractAction(null, icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                delegate.actionPerformed(e);
            }
        };
        action.setEnabled(component.isEnabled());

        final JToolBar toolBar = new JToolBar();
        toolBar.add(action);

        component.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolBar);
        component.addPropertyChangeListener("enabled", e -> action.setEnabled(component.isEnabled()));
    }

    public static void addOpenDirectoryAction(@NotNull JTextField component, @NotNull String title) {
        addOpenAction(component, e -> {
            final JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle(title);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
                component.setText(chooser.getSelectedFile().toString());
            }
        });
    }

    public static void delegateAction(@NotNull JComponent source, @NotNull JComponent target, @NotNull String targetActionKey, int targetCondition) {
        final InputMap inputMap = target.getInputMap(targetCondition);

        for (KeyStroke keyStroke : inputMap.allKeys()) {
            if (targetActionKey.equals(inputMap.get(keyStroke))) {
                delegateAction(source, keyStroke, target, targetActionKey);
            }
        }
    }

    public static void putAction(@NotNull JComponent target, int condition, @NotNull KeyStroke keystroke, @NotNull Action action) {
        target.getInputMap(condition).put(keystroke, action);
        target.getActionMap().put(action, action);
    }

    public static void installContextMenu(@NotNull JTree tree, @NotNull JPopupMenu menu) {
        installContextMenu(tree, menu, new SelectionProvider<JTree, TreePath>() {
            @Nullable
            @Override
            public TreePath getSelection(@NotNull JTree component, @Nullable MouseEvent event) {
                if (event != null) {
                    final TreePath path = component.getPathForLocation(event.getX(), event.getY());

                    if (path != null) {
                        return path;
                    }
                }

                return component.getSelectionPath();
            }

            @NotNull
            @Override
            public Point getSelectionLocation(@NotNull JTree component, @NotNull TreePath selection, @Nullable MouseEvent event) {
                if (event != null) {
                    return event.getPoint();
                } else {
                    final Rectangle bounds = Objects.requireNonNull(component.getPathBounds(selection));
                    return new Point(bounds.x, bounds.y + bounds.height);
                }
            }

            @Override
            public void setSelection(@NotNull JTree component, @NotNull TreePath selection, @Nullable MouseEvent event) {
                component.setSelectionPath(selection);

                if (event == null) {
                    component.scrollPathToVisible(selection);
                }
            }
        });
    }

    public static void installContextMenu(@NotNull JTabbedPane pane, @NotNull JPopupMenu menu) {
        installContextMenu(pane, menu, new SelectionProvider<JTabbedPane, Integer>() {
            @Nullable
            @Override
            public Integer getSelection(@NotNull JTabbedPane component, @Nullable MouseEvent event) {
                final int index;

                if (event != null) {
                    index = component.indexAtLocation(event.getX(), event.getY());
                } else {
                    index = component.getSelectedIndex();
                }

                return index < 0 ? null : index;
            }

            @NotNull
            @Override
            public Point getSelectionLocation(@NotNull JTabbedPane component, @NotNull Integer selection, @Nullable MouseEvent event) {
                if (event != null) {
                    return event.getPoint();
                } else {
                    final Rectangle bounds = Objects.requireNonNull(component.getBoundsAt(selection));
                    return new Point(bounds.x, bounds.y + bounds.height);
                }
            }

            @Override
            public void setSelection(@NotNull JTabbedPane component, @NotNull Integer selection, @Nullable MouseEvent event) {
                component.setSelectedIndex(selection);
            }
        });
    }

    public static void installContextMenu(@NotNull JComponent component, @NotNull JPopupMenu menu) {
        if (component instanceof JTree tree) {
            installContextMenu(tree, menu);
            return;
        } else if (component instanceof JTabbedPane pane) {
            installContextMenu(pane, menu);
            return;
        }

        installContextMenu(component, menu, new SelectionProvider<>() {
            @NotNull
            @Override
            public Object getSelection(@NotNull JComponent component, @Nullable MouseEvent event) {
                return component;
            }

            @NotNull
            @Override
            public Point getSelectionLocation(@NotNull JComponent component, @NotNull Object selection, @Nullable MouseEvent event) {
                if (event != null) {
                    return event.getPoint();
                } else {
                    final Rectangle bounds = component.getBounds();
                    return new Point(bounds.x, bounds.y + bounds.height);
                }
            }

            @Override
            public void setSelection(@NotNull JComponent component, @NotNull Object selection, @Nullable MouseEvent event) {
                // do nothing
            }
        });
    }

    public static <T extends JComponent, U> void installContextMenu(
        @NotNull T component,
        @NotNull JPopupMenu menu,
        @NotNull SelectionProvider<T, U> provider
    ) {
        component.setComponentPopupMenu(menu);
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final U selection = provider.getSelection(component, e);

                    if (selection != null) {
                        provider.setSelection(component, selection, e);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final U selection = provider.getSelection(component, e);

                    if (selection != null) {
                        final Point location = provider.getSelectionLocation(component, selection, e);

                        menu.show(component, location.x, location.y);
                    }
                }
            }
        });

        putAction(component, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final U selection = provider.getSelection(component, null);

                if (selection != null) {
                    provider.setSelection(component, selection, null);

                    final Point location = provider.getSelectionLocation(component, selection, null);

                    menu.show(component, location.x, location.y);
                }
            }
        });
    }

    @NotNull
    public static String getLabelWithIndexMnemonic(@NotNull String label, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("negative index: " + index);
        } else if (index < 10) {
            return "&%d. %s".formatted(index < 9 ? index + 1 : 0, label);
        } else if (index < 36) {
            return "&%c. %s".formatted(index - 10 + 'A', label);
        } else {
            return label;
        }
    }

    public static void showErrorDialog(@NotNull Throwable throwable) {
        showErrorDialog(throwable, "An error occurred during program execution");
    }

    public static void showErrorDialog(@NotNull Throwable throwable, @NotNull String title) {
        showErrorDialog(JOptionPane.getRootFrame(), throwable, title);
    }

    public static void showErrorDialog(@Nullable Window parent, @NotNull Throwable throwable) {
        showErrorDialog(parent, throwable, "An error occurred during program execution");
    }

    public static void showErrorDialog(@Nullable Window parent, @NotNull Throwable throwable, @NotNull String title) {
        throwable.printStackTrace();
        new ExceptionDialog(throwable, title).showDialog(parent);
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

    @NotNull
    public static JScrollPane createBorderlessScrollPane(@Nullable Component view) {
        return new JScrollPane(view) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createEmptyBorder());
            }
        };
    }

    @NotNull
    public static JScrollPane createScrollPane(@Nullable Component view, int top, int left, int bottom, int right) {
        return new JScrollPane(view) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, UIColor.SHADOW));
            }
        };
    }

    @NotNull
    public static JLabel createBoldLabel() {
        return createBoldLabel("");
    }

    @NotNull
    public static JLabel createBoldLabel(@NotNull String text) {
        return new JLabel(text) {
            @Override
            public void updateUI() {
                setFont(null);
                super.updateUI();
                setFont(getFont().deriveFont(Font.BOLD));
            }
        };
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

    @NotNull
    public static String formatDuration(@NotNull Duration duration) {
        return "%d:%02d".formatted(duration.toMinutes(), duration.toSecondsPart());
    }

    @NotNull
    public static DataFlavor createLocalDataFlavor(@NotNull Class<?> cls) {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + cls.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error constructing flavor for " + cls, e);
        }
    }

    @NotNull
    public static String getTextForAccelerator(@NotNull KeyStroke accelerator) {
        final StringBuilder sb = new StringBuilder();

        final int modifiers = accelerator.getModifiers();
        if (modifiers != 0) {
            sb.append(InputEvent.getModifiersExText(modifiers)).append('+');
        }

        final int keyCode = accelerator.getKeyCode();
        if (keyCode != 0) {
            sb.append(KeyEvent.getKeyText(keyCode));
        } else {
            sb.append(accelerator.getKeyChar());
        }

        return sb.toString();
    }

    public static void setCursor(@NotNull Component component, @Nullable Cursor cursor) {
        if (!component.isCursorSet() || component.getCursor() != cursor) {
            component.setCursor(cursor);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, X extends Throwable> T invokeAndWait(@NotNull ThrowableSupplier<T, X> supplier) throws X {
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }

        try {
            final Object[] result = new Object[1];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result[0] = supplier.get();
                } catch (Throwable e) {
                    IOUtils.sneakyThrow(e);
                }
            });
            return (T) result[0];
        } catch (InterruptedException e) {
            return IOUtils.sneakyThrow(e);
        } catch (InvocationTargetException e) {
            throw (X) e.getCause();
        }
    }

    public interface SelectionProvider<T extends JComponent, U> {
        @Nullable
        U getSelection(@NotNull T component, @Nullable MouseEvent event);

        @NotNull
        Point getSelectionLocation(@NotNull T component, @NotNull U selection, @Nullable MouseEvent event);

        void setSelection(@NotNull T component, @NotNull U selection, @Nullable MouseEvent event);
    }
}
