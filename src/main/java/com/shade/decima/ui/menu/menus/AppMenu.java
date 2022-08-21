package com.shade.decima.ui.menu.menus;

import com.shade.platform.ui.menus.Menu;
import com.shade.platform.ui.menus.MenuRegistration;

import static com.shade.decima.ui.menu.MenuConstants.*;

public interface AppMenu {
    @MenuRegistration(id = APP_MENU_FILE_ID, name = "&File", order = 1000)
    class FileMenu extends Menu {}

    @MenuRegistration(id = APP_MENU_EDIT_ID, name = "&Edit", order = 2000)
    class EditMenu extends Menu {}

    @MenuRegistration(id = APP_MENU_HELP_ID, name = "&Help", order = 3000)
    class HelpMenu extends Menu {}
}
