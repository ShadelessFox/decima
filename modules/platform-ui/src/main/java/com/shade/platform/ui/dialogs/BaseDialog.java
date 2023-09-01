package com.shade.platform.ui.dialogs;

import com.shade.platform.model.data.DataKey;
import com.shade.platform.ui.controls.Mnemonic;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;

public abstract class BaseDialog implements ActionListener {
    public static final DataKey<ButtonDescriptor> DESCRIPTOR_KEY = new DataKey<>("descriptor", ButtonDescriptor.class);

    public static final ButtonDescriptor BUTTON_OK = new ButtonDescriptor("ok", "OK", "ok");
    public static final ButtonDescriptor BUTTON_CANCEL = new ButtonDescriptor("cancel", "Cancel", "cancel");
    public static final ButtonDescriptor BUTTON_APPLY = new ButtonDescriptor("apply", "&Apply", null);
    public static final ButtonDescriptor BUTTON_PERSIST = new ButtonDescriptor("ok", "&Persist", null);
    public static final ButtonDescriptor BUTTON_SAVE = new ButtonDescriptor("ok", "&Save", null);
    public static final ButtonDescriptor BUTTON_COPY = new ButtonDescriptor("copy", "&Copy", null);

    protected final String title;

    private JDialog dialog;
    private ButtonDescriptor result;

    public BaseDialog(@NotNull String title) {
        this.title = title;
    }

    @Nullable
    public ButtonDescriptor showDialog(@Nullable Window owner) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

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
        if (dialog != null) {
            dialog.dispose();
        }
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

    @NotNull
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{BUTTON_OK, BUTTON_CANCEL};
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
    protected JComponent createButtonsPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0,alignx right"));

        for (ButtonDescriptor descriptor : getButtons()) {
            final Mnemonic mnemonic = Mnemonic.extract(descriptor.label());
            final JButton button = new JButton();

            if (mnemonic != null) {
                button.setText(mnemonic.text());

                // Don't assign mnemonic for the default button - it's already assigned to the ENTER key
                if (descriptor != getDefaultButton()) {
                    button.setMnemonic(mnemonic.key());
                    button.setDisplayedMnemonicIndex(mnemonic.index());
                }
            } else {
                button.setText(descriptor.label());
            }

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

    protected void configureContentPane(@NotNull JComponent pane) {
        pane.setLayout(new MigLayout("ins dialog", "[grow,fill]", "[grow,fill][]"));
        pane.add(createContentsPane(), "wrap");
        pane.add(createButtonsPane());
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
    protected JDialog createDialog(@Nullable Window owner) {
        final JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);

        configureContentPane((JComponent) dialog.getContentPane());
        configureRootPane(dialog.getRootPane());

        dialog.pack();
        dialog.setMinimumSize(getMinimumSize());
        dialog.setPreferredSize(dialog.getMinimumSize());
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

    @Nullable
    protected Dimension getMinimumSize() {
        return null;
    }

    public record ButtonDescriptor(@NotNull String id, @NotNull String label, @Nullable String tag) {}
}
