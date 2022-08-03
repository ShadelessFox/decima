package com.shade.decima.ui.menu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MenuItemRegistration {
    /**
     * An identifier of the parent menu or menu item.
     * <p>
     * By default uses identifier of the context menu.
     *
     * @see MenuConstants#APP_MENU_FILE_ID
     */
    String parent() default MenuConstants.CTX_MENU_ID;

    /**
     * An identifier of this menu item.
     * <p>
     * May be empty if this menu item doesn't have child items.
     */
    String id() default "";

    /**
     * Display name of this menu item.
     * <p>
     * May contain a single mnemonic character denoted by the {@code &} symbol: {@code E&xit} will be rendered
     * as E<u>x</u>it and will allow the user to activate this item using the {@code Alt+X} keyboard shortcut.
     * <p>
     * May be empty if the name is determined at run-time using {@link MenuItem#getName(MenuItemContext)}.
     */
    String name() default "";

    /**
     * Display icon name of this menu item retrieved through the {@link javax.swing.UIManager}.
     *
     * @see javax.swing.UIManager#getIcon(Object)
     */
    String icon() default "";

    /**
     * Keystroke of this item.
     * <p>
     * Represents a sequence of keyboard keys. Pressing them simultaneously will
     * activate this item.
     *
     * @see javax.swing.KeyStroke#getKeyStroke(String)
     */
    String keystroke() default "";

    /**
     * A group this item belongs to.
     * <p>
     * Group name must be of the format {@code "order,id"} where {@code order} is
     * an integer that denotes the order of that group within the owner menu or menu item,
     * and {@code id} is an identifier of the group itself.
     * <p>
     * Groups are using for visually combining several items into one group
     * separated from other actions using separator.
     *
     * @see MenuConstants#APP_MENU_FILE_GROUP_EXIT
     */
    String group();

    /**
     * A number representing the order of this item within the specified {@link #group()}.
     */
    int order();
}
