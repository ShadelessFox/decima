package com.shade.decima.ui.action.impl.editors;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "&Close", description = "Close currently focused editor", accelerator = "ctrl F4")
@ActionContribution(path = "popup:editor", position = 1)
public class CloseAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final EditorManager manager = Application.getFrame().getEditorManager();
        final Editor editor = manager.getActiveEditor();

        if (editor != null) {
            manager.closeEditor(editor);
        }
    }
}
