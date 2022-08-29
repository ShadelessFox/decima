package com.shade.decima.ui.editor.binary;

import com.shade.decima.ui.editor.NavigatorEditorInput;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HexFormat;

public class BinaryEditor implements Editor {
    private static final int BYTES_PER_LINE = 16;

    private final NavigatorEditorInput input;
    private final JTextArea placeholder;

    public BinaryEditor(@NotNull NavigatorEditorInput input) {
        this.input = input;
        this.placeholder = new JTextArea();

        final Font font = placeholder.getFont();
        this.placeholder.setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));
        this.placeholder.setEditable(false);
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                final NavigatorFileNode node = input.getNode();
                final byte[] data = node.getPackfile().extract(node.getHash());

                return encode(data);
            }

            @Override
            protected void done() {
                final String data;

                try {
                    data = get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                placeholder.setText(data);
                placeholder.setCaretPosition(0);
            }
        }.execute();

        final JScrollPane pane = new JScrollPane(placeholder);
        pane.setBorder(null);

        return pane;
    }

    @NotNull
    @Override
    public EditorInput getInput() {
        return input;
    }

    @Override
    public void setFocus() {
        placeholder.requestFocusInWindow();
    }

    @NotNull
    private String encode(@NotNull byte[] data) {
        final StringBuilder sb = new StringBuilder();
        final HexFormat format = HexFormat.of().withUpperCase();
        final int prefix = 8 - (int) Math.ceil(Math.log(data.length) / Math.log(16));

        for (int i = 0; i < data.length; i += BYTES_PER_LINE) {
            final int length = Math.min(data.length - i, BYTES_PER_LINE);

            sb.append(format.toHexDigits(i), prefix, 8);
            sb.append(": ");

            for (int j = 0; j < BYTES_PER_LINE; j++) {
                if (j == BYTES_PER_LINE / 2) {
                    sb.append("  ");
                }

                if (j < length) {
                    format.toHexDigits(sb, data[i + j]);
                } else {
                    sb.append("  ");
                }

                sb.append(' ');
            }

            sb.append(' ');

            for (int j = 0; j < length; j++) {
                final byte b = data[i + j];
                if (b >= ' ' && b <= '~') {
                    sb.append((char) b);
                } else {
                    sb.append('.');
                }
            }

            if (i + BYTES_PER_LINE < data.length) {
                sb.append('\n');
            }
        }

        return sb.toString();
    }
}
