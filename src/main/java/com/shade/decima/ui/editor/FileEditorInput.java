package com.shade.decima.ui.editor;

import com.shade.util.NotNull;

import java.nio.file.Path;

public interface FileEditorInput extends ProjectEditorInput {
    @NotNull
    Path getPath();
}
