package com.shade.decima.ui.editor.html;

import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorProvider;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class HtmlEditorProvider implements EditorProvider {
    @NotNull
    @Override
    public Editor createEditor(@NotNull EditorInput input) {
        return new HtmlEditor((HtmlEditorInput) input);
    }

    @NotNull
    @Override
    public Match matches(@NotNull EditorInput input) {
        return input instanceof HtmlEditorInput ? Match.PRIMARY : Match.NONE;
    }

    @NotNull
    @Override
    public String getName() {
        return "HTML";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("Tree.leafIcon");
    }
}
