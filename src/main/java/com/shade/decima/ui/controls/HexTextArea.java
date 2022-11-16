package com.shade.decima.ui.controls;

import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HexFormat;

public class HexTextArea extends JTextArea {
    private static final int BYTES_PER_LINE = 16;

    public HexTextArea() {
        final Font font = getFont();

        setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));
        setEditable(false);
        setDropTarget(null);
    }

    public void setData(@NotNull byte[] data) {
        setText(encode(data));
        setCaretPosition(0);
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
