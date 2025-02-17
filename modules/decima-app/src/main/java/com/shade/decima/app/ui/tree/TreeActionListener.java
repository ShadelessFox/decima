package com.shade.decima.app.ui.tree;

import java.util.EventListener;

public interface TreeActionListener extends EventListener {
    void treePathSelected(TreeActionEvent event);
}
