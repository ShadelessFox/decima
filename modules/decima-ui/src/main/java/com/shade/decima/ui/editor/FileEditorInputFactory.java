package com.shade.decima.ui.editor;

import com.shade.platform.model.ElementFactory;
import com.shade.platform.model.SaveableElement;
import com.shade.util.NotNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@ElementFactory.Registration(FileEditorInputFactory.ID)
public class FileEditorInputFactory implements ElementFactory {
    public static final String ID = "com.shade.decima.ui.editor.FileEditorInputFactory";

    @NotNull
    @Override
    public SaveableElement createElement(@NotNull Map<String, Object> state) {
        return new FileEditorInputLazy(
            UUID.fromString((String) state.get("project")),
            Path.of((String) state.get("path"))
        );
    }
}
