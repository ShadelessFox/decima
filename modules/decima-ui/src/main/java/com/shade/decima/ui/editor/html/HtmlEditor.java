package com.shade.decima.ui.editor.html;

import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.util.UIUtils;
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
        ss.addRule("h1 { font-size: 2em; }");
        ss.addRule("h2 { font-size: 1.5em; }");
        ss.addRule("h3 { font-size: 1.25em; }");
        ss.addRule("h4 { font-size: 1em; }");

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

        return UIUtils.createBorderlessScrollPane(editor);
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
