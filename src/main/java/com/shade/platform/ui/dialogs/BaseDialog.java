package com.shade.platform.ui.dialogs;

import com.shade.platform.model.data.DataKey;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Objects;

public abstract class BaseDialog implements ActionListener {
    public static final DataKey<ButtonDescriptor> DESCRIPTOR_KEY = new DataKey<>("descriptor", ButtonDescriptor.class);

    public static final ButtonDescriptor BUTTON_OK = new ButtonDescriptor("ok", "OK", "ok");
    public static final ButtonDescriptor BUTTON_CANCEL = new ButtonDescriptor("cancel", "Cancel", "cancel");
    public static final ButtonDescriptor BUTTON_PERSIST = new ButtonDescriptor("persist", "Persist", null);

    protected final String title;
    protected final List<ButtonDescriptor> buttons;

    private JDialog dialog;
    private ButtonDescriptor result;

    public BaseDialog(@NotNull String title, @NotNull List<ButtonDescriptor> buttons) {
        this.title = title;
        this.buttons = buttons;
    }

    public BaseDialog(@NotNull String title) {
        this(title, List.of(BUTTON_OK, BUTTON_CANCEL));
    }

    @Nullable
    public ButtonDescriptor showDialog(@Nullable JFrame owner) {
        if (dialog != null) {
            throw new IllegalStateException("Dialog is open");
        }

        dialog = createDialog(owner);
        dialog.setVisible(true);

        dialog.getContentPane().removeAll();
        dialog.dispose();
        dialog = null;

        return result;
    }

    public void close() {
        dialog.setVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton button) {
            buttonPressed(Objects.requireNonNull(DESCRIPTOR_KEY.get(button), "Invalid button"));
        }
    }

    @NotNull
    protected abstract JComponent createContentsPane();

    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        result = descriptor;
        close();
    }

    @Nullable
    protected ButtonDescriptor getDefaultButton() {
        return BUTTON_OK;
    }

    @Nullable
    protected JComponent getDefaultComponent() {
        return null;
    }

    protected JDialog getDialog() {
        return dialog;
    }

    @NotNull
    protected Component createButtonsPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0,alignx right"));

        for (ButtonDescriptor descriptor : buttons) {
            final JButton button = new JButton(descriptor.label());

            configureButton(button, descriptor);

            if (descriptor.tag() != null) {
                panel.add(button, "tag " + descriptor.tag());
            } else {
                panel.add(button);
            }
        }

        return panel;
    }

    protected void configureButton(@NotNull JButton button, @NotNull ButtonDescriptor descriptor) {
        button.putClientProperty(DESCRIPTOR_KEY, descriptor);
        button.addActionListener(this);

        if (getDefaultButton() == descriptor) {
            button.addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                    final JRootPane root = SwingUtilities.getRootPane(button);
                    if (root != null) {
                        root.setDefaultButton(button);
                    }
                }
            });
        }
    }

    protected void configureRootPane(@NotNull JRootPane pane) {
        final InputMap im = pane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "cancel");

        final ActionMap am = pane.getActionMap();
        am.put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonPressed(BUTTON_CANCEL);
            }
        });
    }

    @NotNull
    protected JDialog createDialog(@Nullable JFrame owner) {
        final JDialog dialog = new JDialog(owner, title, true);

        final JComponent contentPane = (JComponent) dialog.getContentPane();
        contentPane.setLayout(new MigLayout("ins dialog", "[grow,fill]", "[grow,fill][]"));
        contentPane.add(createContentsPane(), "wrap");
        contentPane.add(createButtonsPane());

        configureRootPane(dialog.getRootPane());

        dialog.pack();
        dialog.setMinimumSize(dialog.getMinimumSize());
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(owner);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                final JComponent component = getDefaultComponent();
                if (component != null) {
                    component.requestFocusInWindow();
                }
            }
        });

        return dialog;
    }

    public static record ButtonDescriptor(@NotNull String id, @NotNull String label, @Nullable String tag) {}
}
