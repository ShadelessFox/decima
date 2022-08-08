package com.shade.decima.ui.menu;

public interface MenuConstants {
    // @formatter:off
    String CTX_MENU_ID                              = "menu.ctx";
    String APP_MENU_ID                              = "menu.app";

    // Application Menu: File
    String APP_MENU_FILE_ID                         = APP_MENU_ID + ".file";
    String APP_MENU_FILE_GROUP_OPEN                 = "1000," + APP_MENU_FILE_ID + ".open";
    String APP_MENU_FILE_GROUP_SAVE                 = "2000," + APP_MENU_FILE_ID + ".save";
    String APP_MENU_FILE_GROUP_EXIT                 = "3000," + APP_MENU_FILE_ID + ".exit";

    // Application Menu: File / New
    String APP_MENU_FILE_NEW_ID                     = APP_MENU_FILE_ID + ".new";
    String APP_MENU_FILE_NEW_GROUP_GENERAL          = "1000," + APP_MENU_FILE_NEW_ID + ".general";

    // Application Menu: Edit
    String APP_MENU_EDIT_ID                         = APP_MENU_ID + ".edit";
    String APP_MENU_EDIT_GROUP_GENERAL              = "1000," + APP_MENU_EDIT_ID + ".general";

    // Application Menu: Help
    String APP_MENU_HELP_ID                         = APP_MENU_ID + ".help";
    String APP_MENU_HELP_GROUP_ABOUT                = "1000," + APP_MENU_HELP_ID + ".about";

    // Context Menu: Navigator
    String CTX_MENU_NAVIGATOR_ID                    = CTX_MENU_ID + ".navigator";
    String CTX_MENU_NAVIGATOR_GROUP_GENERAL         = "1000," + CTX_MENU_NAVIGATOR_ID + ".general";
    String CTX_MENU_NAVIGATOR_GROUP_PROJECT         = "2000," + CTX_MENU_NAVIGATOR_ID + ".project";
    String CTX_MENU_NAVIGATOR_GROUP_EDIT            = "3000," + CTX_MENU_NAVIGATOR_ID + ".edit";

    // Context Menu: Editor Stack
    String CTX_MENU_EDITOR_STACK_ID              = CTX_MENU_ID + ".editorStack";
    String CTX_MENU_EDITOR_STACK_GROUP_CLOSE     = "1000," + CTX_MENU_EDITOR_STACK_ID + ".close";
    String CTX_MENU_EDITOR_STACK_GROUP_GENERAL   = "2000," + CTX_MENU_EDITOR_STACK_ID + ".general";

    // Context Menu: Property Editor
    String CTX_MENU_PROPERTY_EDITOR_ID              = CTX_MENU_ID + ".propertyEditor";
    String CTX_MENU_PROPERTY_EDITOR_GROUP_GENERAL   = "1000," + CTX_MENU_PROPERTY_EDITOR_ID + ".general";

    // @formatter:on
}
