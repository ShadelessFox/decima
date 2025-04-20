package com.shade.decima.ui.editor.binary;

import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorProvider;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class BinaryEditorProvider implements EditorProvider {
    @NotNull
    @Override
    public Editor createEditor(@NotNull EditorInput input) {
        if (input instanceof NodeEditorInput) {
            return new BinaryEditor((NodeEditorInput) input);
        } else {
            return new BinaryEditor((FileEditorInput) input);
        }
    }

    @NotNull
    @Override
    public Match matches(@NotNull EditorInput input) {
        if (input instanceof NodeEditorInput i) {
            return i.getNode().getExtension().equals("stream") ? Match.PRIMARY : Match.APPLIES;
        } else if (input instanceof FileEditorInput) {
            return Match.APPLIES;
        } else {
            return Match.NONE;
        }
    }

    @NotNull
    @Override
    public String getName() {
        return "Binary Editor";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("File.binaryIcon");
    }
}
