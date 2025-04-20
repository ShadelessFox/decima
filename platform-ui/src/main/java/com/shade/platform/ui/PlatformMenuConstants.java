package com.shade.platform.ui;

import com.shade.platform.ui.menus.MenuManager;

public interface PlatformMenuConstants {
    // @formatter:off

    String CTX_MENU_ID = MenuManager.CTX_MENU_ID;
    String APP_MENU_ID = MenuManager.APP_MENU_ID;
    String BAR_MENU_ID = MenuManager.BAR_MENU_ID;

    // Context Menu: Editor Stack
    String CTX_MENU_EDITOR_STACK_ID            = CTX_MENU_ID + ".editorStack";
    String CTX_MENU_EDITOR_STACK_GROUP_GENERAL = "3000," + CTX_MENU_EDITOR_STACK_ID + ".general";
    String CTX_MENU_EDITOR_STACK_GROUP_SPLIT   = "2000," + CTX_MENU_EDITOR_STACK_ID + ".split";
    String CTX_MENU_EDITOR_STACK_GROUP_CLOSE   = "1000," + CTX_MENU_EDITOR_STACK_ID + ".close";

    // @formatter:on
}
