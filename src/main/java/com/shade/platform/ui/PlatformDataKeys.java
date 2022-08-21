package com.shade.platform.ui;

import com.shade.platform.model.data.DataKey;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.stack.EditorStack;

public interface PlatformDataKeys {
    DataKey<Editor> EDITOR_KEY = new DataKey<>("editor", Editor.class);
    DataKey<EditorStack> EDITOR_STACK_KEY = new DataKey<>("editorStack", EditorStack.class);
    DataKey<EditorManager> EDITOR_MANAGER_KEY = new DataKey<>("editorManager", EditorManager.class);
    DataKey<Object> SELECTION_KEY = new DataKey<>("selection", Object.class);
}
