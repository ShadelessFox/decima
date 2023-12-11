package com.shade.platform.ui.controls;

public interface CommonTextAttributes {
    TextAttributes ERROR_ATTRIBUTES = new TextAttributes("Text.errorForeground", TextAttributes.Style.PLAIN);
    TextAttributes MODIFIED_ATTRIBUTES = new TextAttributes("Text.modifiedForeground", TextAttributes.Style.PLAIN);
    TextAttributes IDENTIFIER_ATTRIBUTES = new TextAttributes("Text.identifierForeground", TextAttributes.Style.PLAIN);
    TextAttributes NUMBER_ATTRIBUTES = new TextAttributes("Text.numberForeground", TextAttributes.Style.PLAIN);
    TextAttributes STRING_TEXT_ATTRIBUTES = new TextAttributes("Text.stringForeground", TextAttributes.Style.PLAIN);
    TextAttributes STRING_ESCAPE_ATTRIBUTES = new TextAttributes("Text.stringEscapeForeground", TextAttributes.Style.BOLD);
}
