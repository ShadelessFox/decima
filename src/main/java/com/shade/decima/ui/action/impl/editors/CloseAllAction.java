package com.shade.decima.ui.action.impl.editors;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.action.ActionContribution;
import com.shade.decima.ui.action.ActionRegistration;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionRegistration(name = "Close &All", description = "Close all editors")
@ActionContribution(path = "popup:editor", position = 2)
public class CloseAllAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        final EditorManager manager = Application.getFrame().getEditorManager();

        for (Editor editor : manager.getEditors()) {
            manager.closeEditor(editor);
        }
    }
}
