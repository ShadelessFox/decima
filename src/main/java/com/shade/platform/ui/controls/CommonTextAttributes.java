package com.shade.platform.ui.controls;

import javax.swing.*;

public interface CommonTextAttributes {
    TextAttributes MODIFIED_ATTRIBUTES = new TextAttributes(UIManager.getColor("Navigator.modifiedForeground"), TextAttributes.Style.PLAIN);
    TextAttributes IDENTIFIER_ATTRIBUTES = new TextAttributes(UIManager.getColor("CoreEditor.identifierForeground"), TextAttributes.Style.PLAIN);
    TextAttributes NUMBER_ATTRIBUTES = new TextAttributes(UIManager.getColor("CoreEditor.numberForeground"), TextAttributes.Style.PLAIN);
    TextAttributes STRING_TEXT_ATTRIBUTES = new TextAttributes(UIManager.getColor("CoreEditor.stringForeground"), TextAttributes.Style.PLAIN);
    TextAttributes STRING_ESCAPE_ATTRIBUTES = new TextAttributes(UIManager.getColor("CoreEditor.stringEscapeForeground"), TextAttributes.Style.BOLD);
}
