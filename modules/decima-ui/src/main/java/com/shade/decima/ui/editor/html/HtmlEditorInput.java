package com.shade.decima.ui.editor.html;

import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public class HtmlEditorInput implements EditorInput {
    private final String title;
    private final String body;

    public HtmlEditorInput(@NotNull String title, @NotNull String body) {
        this.title = title;
        this.body = body;
    }

    @NotNull
    public String getBody() {
        return body;
    }

    @NotNull
    @Override
    public String getName() {
        return title;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean representsSameResource(@NotNull EditorInput other) {
        if (other instanceof HtmlEditorInput o) {
            return title.equals(o.title)
                && body.equals(o.body);
        }
        return equals(other);
    }
}
