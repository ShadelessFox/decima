package com.shade.decima.ui.editor;

import com.shade.platform.model.ElementFactory;
import com.shade.util.NotNull;

import java.util.Map;

@ElementFactory.Registration(NodeEditorInputFactory.ID)
public class NodeEditorInputFactory implements ElementFactory {
    public static final String ID = "com.shade.decima.ui.editor.NodeEditorInputFactory";

    @NotNull
    @Override
    public NodeEditorInputLazy createElement(@NotNull Map<String, Object> state) {
        return new NodeEditorInputLazy(
            (String) state.get("project"),
            (String) state.get("packfile"),
            (String) state.get("resource")
        );
    }
}
