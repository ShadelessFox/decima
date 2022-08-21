package com.shade.platform.ui.controls;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public record Mnemonic(@NotNull String text, int key, int index) {
    @Nullable
    public static Mnemonic extract(@NotNull String name) {
        final int index = name.indexOf('&');
        if (index >= 0 && name.length() > index + 1 && name.charAt(index + 1) != '&') {
            return new Mnemonic(name.substring(0, index) + name.substring(index + 1), name.charAt(index + 1), index);
        } else {
            return null;
        }
    }

    public void apply(@NotNull AbstractButton button) {
        button.setText(text);
        button.setMnemonic(key);
        button.setDisplayedMnemonicIndex(index);
    }
}
