/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.view;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;

/**
 * Interface for managing the items in a menu.
 * <p>
 * Different menu types support different features:
 * <ol>
 * <li><b>Context menus</b>: Do not support item shortcuts and item icons.
 * <li><b>Options menus</b>: The <b>icon menus</b> do not support item check
 * marks and only show the item's
 * {@link MenuItem#setTitleCondensed(CharSequence) condensed title}. The
 * <b>expanded menus</b> (only available if six or more menu items are visible,
 * reached via the 'More' item in the icon menu) do not show item icons, and
 * item check marks are discouraged.
 * <li><b>Sub menus</b>: Do not support item icons, or nested sub menus.
 * </ol>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about creating menus, read the
 * <a href="https://developer.android.com/guide/topics/ui/menus">Menus</a> developer guide.</p>
 * </div>
 */
public interface Menu {

    /**
     * This is the part of an order integer that the user can provide.
     */
    int USER_MASK = 0x0000ffff;

    /**
     * Bit shift of the user portion of the order integer.
     */
    int USER_SHIFT = 0;

    /**
     * This is the part of an order integer that supplies the category of the
     * item.
     */
    int CATEGORY_MASK = 0xffff0000;

    /**
     * Bit shift of the category portion of the order integer.
     */
    int CATEGORY_SHIFT = 16;

    /**
     * A mask of all supported modifiers for MenuItem's keyboard shortcuts
     */
    int SUPPORTED_MODIFIERS_MASK = KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON;

    /**
     * Value to use for group and item identifier integers when you don't care
     * about them.
     */
    int NONE = 0;

    /**
     * First value for group and item identifier integers.
     */
    int FIRST = 1;

    /**
     * Category code for the order integer for items/groups that are part of a
     * container -- or/add this with your base value.
     */
    int CATEGORY_CONTAINER = 0x00010000;

    /**
     * Category code for the order integer for items/groups that are provided by
     * the system -- or/add this with your base value.
     */
    int CATEGORY_SYSTEM = 0x00020000;

    /**
     * Category code for the order integer for items/groups that are
     * user-supplied secondary (infrequently used) options -- or/add this with
     * your base value.
     */
    int CATEGORY_SECONDARY = 0x00030000;

    /**
     * Category code for the order integer for items/groups that are
     * alternative actions on the data that is currently displayed -- or/add
     * this with your base value.
     */
    int CATEGORY_ALTERNATIVE = 0x00040000;

    /**
     * Flag for {@link #performShortcut}: if set, do not close the menu after
     * executing the shortcut.
     */
    int FLAG_PERFORM_NO_CLOSE = 0x0001;

    /**
     * Flag for {@link #performShortcut(int, KeyEvent, int)}: if set, always
     * close the menu after executing the shortcut. Closing the menu also resets
     * the prepared state.
     */
    int FLAG_ALWAYS_PERFORM_CLOSE = 0x0002;

    /**
     * Add a new item to the menu. This item displays the given title for its
     * label.
     *
     * @param title The text to display for the item.
     * @return The newly added menu item.
     */
    @NonNull
    MenuItem add(CharSequence title);

    /**
     * Add a new item to the menu. This item displays the given title for its
     * label.
     *
     * @param groupId The group identifier that this item should be part of.
     *                This can be used to define groups of items for batch state
     *                changes. Normally use {@link #NONE} if an item should not be in a
     *                group.
     * @param itemId  Unique item ID. Use {@link #NONE} if you do not need a
     *                unique ID.
     * @param order   The order for the item. Use {@link #NONE} if you do not care
     *                about the order. See {@link MenuItem#getOrder()}.
     * @param title   The text to display for the item.
     * @return The newly added menu item.
     */
    @NonNull
    MenuItem add(int groupId, int itemId, int order, CharSequence title);

    /**
     * Add a new sub-menu to the menu. This item displays the given title for
     * its label. To modify other attributes on the submenu's menu item, use
     * {@link SubMenu#getItem()}.
     *
     * @param title The text to display for the item.
     * @return The newly added sub-menu
     */
    @NonNull
    SubMenu addSubMenu(CharSequence title);

    /**
     * Add a new sub-menu to the menu. This item displays the given
     * <var>title</var> for its label. To modify other attributes on the
     * submenu's menu item, use {@link SubMenu#getItem()}.
     * <p>
     * Note that you can only have one level of sub-menus, i.e. you cannot add
     * a subMenu to a subMenu: An {@link UnsupportedOperationException} will be
     * thrown if you try.
     *
     * @param groupId The group identifier that this item should be part of.
     *                This can also be used to define groups of items for batch state
     *                changes. Normally use {@link #NONE} if an item should not be in a
     *                group.
     * @param itemId  Unique item ID. Use {@link #NONE} if you do not need a
     *                unique ID.
     * @param order   The order for the item. Use {@link #NONE} if you do not care
     *                about the order. See {@link MenuItem#getOrder()}.
     * @param title   The text to display for the item.
     * @return The newly added sub-menu
     */
    @NonNull
    SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title);

    /**
     * Remove the item with the given identifier.
     *
     * @param id The item to be removed.  If there is no item with this
     *           identifier, nothing happens.
     */
    void removeItem(int id);

    /**
     * Remove all items in the given group.
     *
     * @param groupId The group to be removed.  If there are no items in this
     *                group, nothing happens.
     */
    void removeGroup(int groupId);

    /**
     * Remove all existing items from the menu, leaving it empty as if it had
     * just been created.
     */
    void clear();

    /**
     * Control whether a particular group of items can show a check mark.  This
     * is similar to calling {@link MenuItem#setCheckable} on all the menu items
     * with the given group identifier, but in addition you can control whether
     * this group contains a mutually-exclusive set items.  This should be called
     * after the items of the group have been added to the menu.
     *
     * @param group     The group of items to operate on.
     * @param checkable Set to true to allow a check mark, false to
     *                  disallow.  The default is false.
     * @param exclusive If set to true, only one item in this group can be
     *                  checked at a time; checking an item will automatically
     *                  uncheck all others in the group.  If set to false, each
     *                  item can be checked independently of the others.
     * @see MenuItem#setCheckable
     * @see MenuItem#setChecked
     */
    void setGroupCheckable(int group, boolean checkable, boolean exclusive);

    /**
     * Show or hide all menu items that are in the given group.
     *
     * @param group   The group of items to operate on.
     * @param visible If true the items are visible, else they are hidden.
     * @see MenuItem#setVisible
     */
    void setGroupVisible(int group, boolean visible);

    /**
     * Enable or disable all menu items that are in the given group.
     *
     * @param group   The group of items to operate on.
     * @param enabled If true the items will be enabled, else they will be disabled.
     * @see MenuItem#setEnabled
     */
    void setGroupEnabled(int group, boolean enabled);

    /**
     * Return whether the menu currently has item items that are visible.
     *
     * @return True if there is one or more item visible,
     * else false.
     */
    boolean hasVisibleItems();

    /**
     * Return the menu item with a particular identifier.
     *
     * @param id The identifier to find.
     * @return The menu item object, or null if there is no item with
     * this identifier.
     */
    @Nullable
    MenuItem findItem(int id);

    /**
     * Get the number of items in the menu.  Note that this will change any
     * times items are added or removed from the menu.
     *
     * @return The item count.
     */
    int size();

    /**
     * Gets the menu item at the given index.
     *
     * @param index The index of the menu item to return.
     * @return The menu item.
     * @throws IndexOutOfBoundsException when {@code index < 0 || >= size()}
     */
    @NonNull
    MenuItem getItem(int index);

    /**
     * Closes the menu, if open.
     */
    void close();

    /**
     * Execute the menu item action associated with the given shortcut
     * character.
     *
     * @param keyCode The keycode of the shortcut key.
     * @param event   Key event message.
     * @param flags   Additional option flags or 0.
     * @return If the given shortcut exists and is shown, returns
     * true; else returns false.
     * @see #FLAG_PERFORM_NO_CLOSE
     */
    boolean performShortcut(int keyCode, @NonNull KeyEvent event, int flags);

    /**
     * Is a keypress one of the defined shortcut keys for this window.
     *
     * @param keyCode the key code from {@link KeyEvent} to check.
     * @param event   the {@link KeyEvent} to use to help check.
     */
    boolean isShortcutKey(int keyCode, @NonNull KeyEvent event);

    /**
     * Execute the menu item action associated with the given menu identifier.
     *
     * @param id    Identifier associated with the menu item.
     * @param flags Additional option flags or 0.
     * @return If the given identifier exists and is shown, returns
     * true; else returns false.
     * @see #FLAG_PERFORM_NO_CLOSE
     */
    boolean performIdentifierAction(int id, int flags);

    /**
     * Control whether the menu should be running in qwerty mode (alphabetic
     * shortcuts) or 12-key mode (numeric shortcuts).
     *
     * @param isQwerty If true the menu will use alphabetic shortcuts; else it
     *                 will use numeric shortcuts.
     */
    void setQwertyMode(boolean isQwerty);

    /**
     * Enable or disable the group dividers.
     */
    void setGroupDividerEnabled(boolean groupDividerEnabled);
}
