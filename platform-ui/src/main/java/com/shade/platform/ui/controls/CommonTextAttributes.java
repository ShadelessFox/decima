package com.shade.platform.ui.controls;

import javax.swing.*;

public interface CommonTextAttributes {
    TextAttributes ERROR_ATTRIBUTES = new TextAttributes(UIManager.getColor("Text.errorForeground"), TextAttributes.Style.PLAIN);
    TextAttributes MODIFIED_ATTRIBUTES = new TextAttributes(UIManager.getColor("Text.modifiedForeground"), TextAttributes.Style.PLAIN);
    TextAttributes IDENTIFIER_ATTRIBUTES = new TextAttributes(UIManager.getColor("Text.identifierForeground"), TextAttributes.Style.PLAIN);
    TextAttributes NUMBER_ATTRIBUTES = new TextAttributes(UIManager.getColor("Text.numberForeground"), TextAttributes.Style.PLAIN);
    TextAttributes STRING_TEXT_ATTRIBUTES = new TextAttributes(UIManager.getColor("Text.stringForeground"), TextAttributes.Style.PLAIN);
    TextAttributes STRING_ESCAPE_ATTRIBUTES = new TextAttributes(UIManager.getColor("Text.stringEscapeForeground"), TextAttributes.Style.BOLD);
}
