package com.shade.decima.ui.editor.binary;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.hex.HexCaret;
import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.decima.ui.controls.hex.impl.DefaultHexModel;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.editor.ProjectEditorInput;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.StatefulEditor;
import com.shade.util.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;

public class BinaryEditor implements Editor, StatefulEditor {
    private final ProjectEditorInput input;
    private final HexEditor editor;

    public BinaryEditor(@NotNull NodeEditorInput input) {
        this((ProjectEditorInput) input);
    }

    public BinaryEditor(@NotNull FileEditorInput input) {
        this((ProjectEditorInput) input);
    }

    private BinaryEditor(@NotNull ProjectEditorInput input) {
        this.input = input;
        this.editor = new HexEditor();
        this.editor.setRowLength(32);

        Application.getMenuService().installPopupMenu(editor, MenuConstants.CTX_MENU_BINARY_EDITOR_ID, key -> switch (key) {
            case "editor" -> this;
            case "project" -> input.getProject();
            default -> null;
        });
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        try {
            final byte[] data;

            if (input instanceof NodeEditorInput i) {
                data = i.getNode().getPackfile().extract(i.getNode().getHash());
            } else if (input instanceof FileEditorInput i) {
                data = Files.readAllBytes(i.getPath());
            } else {
                throw new IllegalArgumentException("Unexpected editor input: " + input);
            }

            editor.setModel(new DefaultHexModel(data));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

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

    @SuppressWarnings("unchecked")
    @Override
    public void loadState(@NotNull Map<String, Object> state) {
        final var caret = (Map<String, Number>) state.get("caret");

        if (caret != null) {
            final int dot = caret.get("dot").intValue();
            final int mark = caret.get("mark").intValue();

            editor.getCaret().setDot(mark);
            editor.getCaret().moveDot(dot);
            editor.scrollSelectionToVisible();
        }
    }

    @Override
    public void saveState(@NotNull Map<String, Object> state) {
        final HexCaret caret = editor.getCaret();
        state.put("caret", Map.of("dot", caret.getDot(), "mark", caret.getMark()));
    }
}
