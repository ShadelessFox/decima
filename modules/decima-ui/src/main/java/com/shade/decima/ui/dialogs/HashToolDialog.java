package com.shade.decima.ui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.util.hash.CRC32C;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.platform.ui.controls.DocumentAdapter;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
        private final JTextField crc32FieldDec;
        private final JTextField crc32FieldHex;
        private final JTextField murmurFieldDec;
        private final JTextField murmurFieldHex;
        private final JTextField inputField;
        private final JToggleButton nullTerminatedButton;

        public ContentPanel() {
            setLayout(new MigLayout("ins dialog,wrap 3", "[][grow,fill,150lp]"));

            crc32FieldDec = new JTextField();
            crc32FieldDec.setEditable(false);

            crc32FieldHex = new JTextField();
            crc32FieldHex.setEditable(false);

            murmurFieldDec = new JTextField();
            murmurFieldDec.setEditable(false);

            murmurFieldHex = new JTextField();
            murmurFieldHex.setEditable(false);

            nullTerminatedButton = new JToggleButton(UIManager.getIcon("Action.nullTerminatorIcon"));
            nullTerminatedButton.setToolTipText("Null-terminated string");
            nullTerminatedButton.addActionListener(e -> update());

            final JToolBar toolBar = new JToolBar();
            toolBar.add(nullTerminatedButton);

            inputField = new JTextField();
            inputField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, toolBar);
            inputField.getDocument().addDocumentListener((DocumentAdapter) e -> update());

            add(new JLabel("Text:"));
            add(inputField, "span 2");

            add(new JLabel("CRC32-C:"));
            add(crc32FieldDec);
            add(crc32FieldHex);

            add(new JLabel("MurmurHash3:"));
            add(murmurFieldDec);
            add(murmurFieldHex);

            update();
        }

        private void update() {
            byte[] data = inputField.getText().getBytes(StandardCharsets.UTF_8);

            if (nullTerminatedButton.isSelected()) {
                data = Arrays.copyOf(data, data.length + 1);
            }

            final var crc32 = CRC32C.calculate(data);
            final var mmh3 = MurmurHash3.mmh3(data)[0];

            crc32FieldDec.setText(Integer.toUnsignedString(crc32));
            crc32FieldHex.setText(String.format("%#010x", crc32));
            murmurFieldDec.setText(Long.toUnsignedString(mmh3));
            murmurFieldHex.setText(String.format("%#018x", mmh3));
        }
    }
}
