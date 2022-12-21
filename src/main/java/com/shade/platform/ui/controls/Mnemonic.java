package com.shade.platform.ui.controls;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public record Mnemonic(@NotNull String text, int key, int index) {
    @Nullable
    public static Mnemonic extract(@NotNull String name) {
        final int index = name.indexOf('&');
        if (index >= 0 && name.length() > index + 1 && name.charAt(index + 1) != '&') {
            return new Mnemonic(
                name.substring(0, index) + name.substring(index + 1),
                Character.toUpperCase(name.charAt(index + 1)),
                index
            );
        } else {
            return null;
        }
    }

    @NotNull
    public static <T extends AbstractButton> T resolve(@NotNull T button) {
        final Mnemonic mnemonic = extract(button.getText());

        if (mnemonic != null) {
            mnemonic.setText(button);
        }

        return button;
    }

    @NotNull
    public static <T extends JLabel> T resolve(@NotNull T label) {
        final Mnemonic mnemonic = extract(label.getText());

        if (mnemonic != null) {
            mnemonic.setText(label);
        }

        return label;
    }

    public void setText(@NotNull AbstractButton button) {
        button.setText(text);
        button.setMnemonic(key);
        button.setDisplayedMnemonicIndex(index);
    }

    public void setText(@NotNull JLabel label) {
        label.setText(text);
        label.setDisplayedMnemonic(key);
        label.setDisplayedMnemonicIndex(index);
    }
}
