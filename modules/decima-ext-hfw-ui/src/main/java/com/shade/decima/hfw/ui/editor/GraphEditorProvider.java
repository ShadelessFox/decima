package com.shade.decima.hfw.ui.editor;

import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorProvider;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.io.IOException;

public class GraphEditorProvider implements EditorProvider {
    @NotNull
    @Override
    public Editor createEditor(@NotNull EditorInput input) {
        try {
            return new GraphEditor((FileEditorInput) input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Match matches(@NotNull EditorInput input) {
        return Match.APPLIES;
    }

    @NotNull
    @Override
    public String getName() {
        return "Graph Editor";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("Node.referenceIcon");
    }
}
