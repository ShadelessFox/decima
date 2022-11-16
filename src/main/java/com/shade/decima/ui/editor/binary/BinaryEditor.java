package com.shade.decima.ui.editor.binary;

import com.shade.decima.ui.controls.HexTextArea;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.util.NotNull;

import javax.swing.*;

public class BinaryEditor implements Editor {
    private final FileEditorInput input;
    private final HexTextArea area;

    public BinaryEditor(@NotNull FileEditorInput input) {
        this.input = input;
        this.area = new HexTextArea();
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() throws Exception {
                final NavigatorFileNode node = input.getNode();
                return node.getPackfile().extract(node.getHash());
            }

            @Override
            protected void done() {
                final byte[] data;

                try {
                    data = get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                area.setData(data);
            }
        }.execute();

        final JScrollPane pane = new JScrollPane(area);
        pane.setBorder(null);

        return pane;
    }

    @NotNull
    @Override
    public EditorInput getInput() {
        return input;
    }

    @Override
    public void setFocus() {
        area.requestFocusInWindow();
    }
}
