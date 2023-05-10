package com.shade.decima.ui.editor.html;

import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

public class HtmlEditor implements Editor {
    private final HtmlEditorInput input;

    private JEditorPane editor;

    public HtmlEditor(@NotNull HtmlEditorInput input) {
        this.input = input;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        final HTMLEditorKit kit = new HTMLEditorKit();
        final StyleSheet ss = kit.getStyleSheet();
        ss.addRule("code { font-size: inherit; font-family: Monospaced; }");
        ss.addRule("ul { margin-left-ltr: 16px; margin-right-ltr: 16px; }");

        editor = new JEditorPane();
        editor.setEditorKit(kit);
        editor.setEditable(false);
        editor.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                IOUtils.unchecked(() -> {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                    return null;
                });
            }
        });
        editor.setText(input.getBody());

        final JScrollPane pane = new JScrollPane(editor);
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
        editor.requestFocusInWindow();
    }

    @Override
    public boolean isFocused() {
        return editor.isFocusOwner();
    }
}
