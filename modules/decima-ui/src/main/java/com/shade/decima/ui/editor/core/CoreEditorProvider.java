package com.shade.decima.ui.editor.core;

import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorProvider;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class CoreEditorProvider implements EditorProvider {
    @NotNull
    @Override
    public Editor createEditor(@NotNull EditorInput input) {
        if (input instanceof NodeEditorInput i) {
            return new CoreEditor(i);
        } else if (input instanceof CoreEditorInput i) {
            return new CoreEditor(i);
        } else if (input instanceof FileEditorInput i) {
            return new CoreEditor(i);
        } else {
            throw new IllegalArgumentException("Unsupported input: " + input);
        }
    }

    @NotNull
    @Override
    public Match matches(@NotNull EditorInput input) {
        if (input instanceof CoreEditorInput) {
            return Match.PRIMARY;
        } else if (input instanceof NodeEditorInput || input instanceof FileEditorInput) {
            return Match.APPLIES;
        } else {
            return Match.NONE;
        }
    }

    @NotNull
    @Override
    public String getName() {
        return "Core Editor";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("File.coreIcon");
    }
}
