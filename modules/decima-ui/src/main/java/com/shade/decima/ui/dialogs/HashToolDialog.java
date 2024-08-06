package com.shade.decima.ui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.util.hash.spi.Hasher;
import com.shade.platform.ui.controls.DocumentAdapter;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HashToolDialog extends JDialog {
    private static final AtomicBoolean VISIBLE = new AtomicBoolean(false);

    public static void open(@Nullable Window owner) {
        if (VISIBLE.compareAndSet(false, true)) {
            new HashToolDialog(owner).setVisible(true);
        }
    }

    public HashToolDialog(@Nullable Window owner) {
        super(owner, "Hash Tool");

        setContentPane(new ContentPanel());
        pack();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(owner);

        UIUtils.putAction(rootPane, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent e) {
                VISIBLE.set(isVisible());
            }
        });
    }

    private static class ContentPanel extends JPanel {
        private final List<HasherInfo> converters = new ArrayList<>();
        private final JTextField inputField;
        private final JToggleButton upperCaseButton;
        private final JToggleButton lowerCaseButton;
        private final JToggleButton nullTerminatedButton;

        public ContentPanel() {
            setLayout(new MigLayout("ins dialog,wrap 3", "[][grow,fill,200lp]"));

            upperCaseButton = new JToggleButton(UIManager.getIcon("Action.upperCaseIcon"));
            upperCaseButton.setToolTipText("Treat as uppercase");

            lowerCaseButton = new JToggleButton(UIManager.getIcon("Action.lowerCaseIcon"));
            lowerCaseButton.setToolTipText("Treat as lowercase");

            nullTerminatedButton = new JToggleButton(UIManager.getIcon("Action.nullTerminatorIcon"));
            nullTerminatedButton.setToolTipText("Include null terminator");

            upperCaseButton.addActionListener(e -> {
                lowerCaseButton.setSelected(false);
                update();
            });
            lowerCaseButton.addActionListener(e -> {
                upperCaseButton.setSelected(false);
                update();
            });
            nullTerminatedButton.addActionListener(e -> update());

            final JToolBar toolBar = new JToolBar();
            toolBar.add(upperCaseButton);
            toolBar.add(lowerCaseButton);
            toolBar.add(nullTerminatedButton);

            inputField = new JTextField();
            inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolBar);
            inputField.getDocument().addDocumentListener((DocumentAdapter) e -> update());

            add(new JLabel("Text:"));
            add(inputField, "span 2");
            add(new JSeparator(), "growx,span 3");

            for (Hasher hasher : Hasher.availableHashers()) {
                final JTextField decField = new JTextField();
                decField.setEditable(false);

                final JTextField hexField = new JTextField();
                hexField.setEditable(false);

                add(new JLabel(hasher.name() + ":"));
                add(decField);
                add(hexField);

                converters.add(new HasherInfo(hasher, decField, hexField));
            }

            update();
        }

        private void update() {
            String text = inputField.getText();

            if (upperCaseButton.isSelected()) {
                text = text.toUpperCase();
            } else if (lowerCaseButton.isSelected()) {
                text = text.toLowerCase();
            }

            byte[] data = text.getBytes(StandardCharsets.UTF_8);

            if (nullTerminatedButton.isSelected()) {
                data = Arrays.copyOf(data, data.length + 1);
            }

            for (HasherInfo info : converters) {
                if (info.hasher instanceof Hasher.ToInt h) {
                    final var value = h.calculate(data);
                    info.decField.setText(Integer.toUnsignedString(value));
                    info.hexField.setText("%#010x".formatted(value));
                } else if (info.hasher instanceof Hasher.ToLong h) {
                    final var value = h.calculate(data);
                    info.decField.setText(Long.toUnsignedString(value));
                    info.hexField.setText("%#018x".formatted(value));
                }
            }
        }
    }

    private record HasherInfo(@NotNull Hasher hasher, @NotNull JTextField decField, @NotNull JTextField hexField) {}
}
