package com.shade.platform.ui.controls;

import com.shade.platform.ui.UIColor;

public interface CommonTextAttributes {
    TextAttributes ERROR_ATTRIBUTES = new TextAttributes(UIColor.named("Text.errorForeground"), TextAttributes.Style.PLAIN);
    TextAttributes MODIFIED_ATTRIBUTES = new TextAttributes(UIColor.named("Text.modifiedForeground"), TextAttributes.Style.PLAIN);
    TextAttributes IDENTIFIER_ATTRIBUTES = new TextAttributes(UIColor.named("Text.identifierForeground"), TextAttributes.Style.PLAIN);
    TextAttributes NUMBER_ATTRIBUTES = new TextAttributes(UIColor.named("Text.numberForeground"), TextAttributes.Style.PLAIN);
    TextAttributes STRING_TEXT_ATTRIBUTES = new TextAttributes(UIColor.named("Text.stringForeground"), TextAttributes.Style.PLAIN);
    TextAttributes STRING_ESCAPE_ATTRIBUTES = new TextAttributes(UIColor.named("Text.stringEscapeForeground"), TextAttributes.Style.BOLD);
    TextAttributes REFERENCE_ATTRIBUTES = new TextAttributes(UIColor.named("Text.referenceForeground"), TextAttributes.Style.PLAIN);
}
