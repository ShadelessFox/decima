package com.shade.platform.ui.editors.lazy;

import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;

/**
 * An editor input can can be unloaded back to its lazy variant.
 *
 * @see LazyEditorInput
 */
public interface UnloadableEditorInput extends EditorInput {
    @NotNull
    LazyEditorInput unloadInput();
}
