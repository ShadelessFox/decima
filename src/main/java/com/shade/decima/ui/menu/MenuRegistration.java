package com.shade.decima.ui.menu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MenuRegistration {
    /**
     * An identifier of the parent menu.
     * <p>
     * By default uses identifier of the main menu of an application.
     *
     * @see MenuConstants#APP_MENU_ID
     */
    String parent() default MenuConstants.APP_MENU_ID;

    /**
     * An identifier of this menu.
     */
    String id();

    /**
     * Display name of this menu item.
     * <p>
     * May contain a single mnemonic character denoted by the {@code &} symbol: {@code &File} will be rendered
     * as <u>F</u>ile and will allow the user to activate this item using the {@code Alt+F} keyboard shortcut.
     * <p>
     * May be empty if the name is determined at run-time using {@link MenuItem#getName(MenuItemContext)}.
     */
    String name();

    /**
     * A number representing the order of this menu within the specified {@link #parent()}.
     */
    int order();
}
