package com.shade.decima.ui.editor;

import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.ElementFactory;
import com.shade.util.NotNull;

import java.util.prefs.Preferences;

@ElementFactory.Registration(NodeEditorInputFactory.ID)
public class NodeEditorInputFactory implements ElementFactory {
    public static final String ID = "com.shade.decima.ui.editor.NodeEditorInputFactory";

    @NotNull
    @Override
    public NodeEditorInputLazy createElement(@NotNull Preferences pref) {
        return new NodeEditorInputLazy(
            IOUtils.getNotNull(pref, "project"),
            IOUtils.getNotNull(pref, "packfile"),
            IOUtils.getNotNull(pref, "resource")
        );
    }
}
