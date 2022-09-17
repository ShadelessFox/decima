package com.shade.decima.ui.editor.core;

import com.shade.decima.ui.editor.NavigatorEditorInput;
import com.shade.decima.ui.editor.binary.BinaryEditor;
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
        try {
            return new CoreEditor((NavigatorEditorInput) input);
        } catch (Exception ignored) {
            return new BinaryEditor((NavigatorEditorInput) input);
        }
    }

    @Override
    public boolean supports(@NotNull EditorInput input) {
        return input instanceof NavigatorEditorInput;
    }

    @NotNull
    @Override
    public String getName() {
        return "Core Editor";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("Navigator.coreIcon");
    }
}
