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

package icyllis.modernui.view.menu;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.view.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class MenuItemImpl implements MenuItem {

    private static final int SHOW_AS_ACTION_MASK = SHOW_AS_ACTION_NEVER |
            SHOW_AS_ACTION_IF_ROOM |
            SHOW_AS_ACTION_ALWAYS;

    private final int mId;
    private final int mGroup;
    private final int mCategoryOrder;
    private final int mOrdering;
    private CharSequence mTitle;
    private CharSequence mTitleCondensed;
    private char mShortcutNumericChar;
    private int mShortcutNumericModifiers = KeyEvent.META_CTRL_ON;
    private char mShortcutAlphabeticChar;
    private int mShortcutAlphabeticModifiers = KeyEvent.META_CTRL_ON;

    /**
     * The icon's drawable which is only created as needed
     */
    private Drawable mIconDrawable;

    private ColorStateList mIconTintList = null;
    private boolean mHasIconTint = false;
    private boolean mNeedToApplyIconTint = false;

    /**
     * The menu to which this item belongs
     */
    private final MenuBuilder mMenu;
    /**
     * If this item should launch a sub menu, this is the sub menu to launch
     */
    private SubMenuBuilder mSubMenu;

    private Runnable mItemCallback;
    private OnMenuItemClickListener mClickListener;

    private int mFlags = ENABLED;
    private static final int CHECKABLE = 0x00000001;
    private static final int CHECKED = 0x00000002;
    private static final int EXCLUSIVE = 0x00000004;
    private static final int HIDDEN = 0x00000008;
    private static final int ENABLED = 0x00000010;
    private static final int IS_ACTION = 0x00000020;

    private int mShowAsAction;

    private View mActionView;
    private ActionProvider mActionProvider;
    private OnActionExpandListener mOnActionExpandListener;
    private boolean mIsActionViewExpanded = false;

    /**
     * Current use case is for context menu: Extra information linked to the
     * View that added this item to the context menu.
     */
    private Object mMenuInfo;

    private CharSequence mContentDescription;
    private CharSequence mTooltipText;

    /**
     * Instantiates this menu item.
     *
     * @param group         Item ordering grouping control. The item will be added after
     *                      all other items whose order is <= this number, and before any
     *                      that are larger than it. This can also be used to define
     *                      groups of items for batch state changes. Normally use 0.
     * @param id            Unique item ID. Use 0 if you do not need a unique ID.
     * @param categoryOrder The ordering for this item.
     * @param title         The text to display for the item.
     */
    MenuItemImpl(@Nonnull MenuBuilder menu, int group, int id, int categoryOrder, int ordering,
                 @Nullable CharSequence title, int showAsAction) {
        mMenu = menu;
        mId = id;
        mGroup = group;
        mCategoryOrder = categoryOrder;
        mOrdering = ordering;
        mTitle = title;
        mShowAsAction = showAsAction;
    }

    /**
     * Invokes the item by calling various listeners or callbacks.
     *
     * @return true if the invocation was handled, false otherwise
     */
    public boolean invoke() {
        if (mClickListener != null &&
                mClickListener.onMenuItemClick(this)) {
            return true;
        }

        if (mMenu.dispatchMenuItemSelected(mMenu, this)) {
            return true;
        }

        if (mItemCallback != null) {
            mItemCallback.run();
            return true;
        }

        return mActionProvider != null && mActionProvider.onPerformDefaultAction();
    }

    @Override
    public boolean isEnabled() {
        return (mFlags & ENABLED) != 0;
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        if (enabled) {
            mFlags |= ENABLED;
        } else {
            mFlags &= ~ENABLED;
        }

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public int getGroupId() {
        return mGroup;
    }

    @Override
    public int getItemId() {
        return mId;
    }

    @Override
    public int getOrder() {
        return mCategoryOrder;
    }

    public int getOrdering() {
        return mOrdering;
    }

    Runnable getCallback() {
        return mItemCallback;
    }

    public MenuItem setCallback(Runnable callback) {
        mItemCallback = callback;
        return this;
    }

    @Override
    public char getAlphabeticShortcut() {
        return mShortcutAlphabeticChar;
    }

    @Override
    public int getAlphabeticModifiers() {
        return mShortcutAlphabeticModifiers;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        if (mShortcutAlphabeticChar == alphaChar) return this;

        mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers) {
        if (mShortcutAlphabeticChar == alphaChar &&
                mShortcutAlphabeticModifiers == alphaModifiers) {
            return this;
        }

        mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);
        mShortcutAlphabeticModifiers = alphaModifiers;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public char getNumericShortcut() {
        return mShortcutNumericChar;
    }

    @Override
    public int getNumericModifiers() {
        return mShortcutNumericModifiers;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar) {
        if (mShortcutNumericChar == numericChar) return this;

        mShortcutNumericChar = numericChar;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar, int numericModifiers) {
        if (mShortcutNumericChar == numericChar && mShortcutNumericModifiers == numericModifiers) {
            return this;
        }

        mShortcutNumericChar = numericChar;
        mShortcutNumericModifiers = numericModifiers;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        mShortcutNumericChar = numericChar;
        mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers,
                                int alphaModifiers) {
        mShortcutNumericChar = numericChar;
        mShortcutNumericModifiers = numericModifiers;
        mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);
        mShortcutAlphabeticModifiers = alphaModifiers;

        mMenu.onItemsChanged(false);

        return this;
    }

    /**
     * @return The active shortcut (based on QWERTY-mode of the menu).
     */
    char getShortcut() {
        return (mMenu.isQwertyMode() ? mShortcutAlphabeticChar : mShortcutNumericChar);
    }

    /**
     * @return The label to show for the shortcut. This includes the chording
     * key (for example 'Menu+a'). Also, any non-human readable
     * characters should be human-readable (for example 'Menu+enter').
     */
    @Nonnull
    String getShortcutLabel() {

        char shortcut = getShortcut();
        if (shortcut == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        final int modifiers =
                mMenu.isQwertyMode() ? mShortcutAlphabeticModifiers : mShortcutNumericModifiers;
        appendModifier(sb, modifiers, KeyEvent.META_CTRL_ON, "Ctrl + ");
        appendModifier(sb, modifiers, KeyEvent.META_ALT_ON, "Alt + ");
        appendModifier(sb, modifiers, KeyEvent.META_SHIFT_ON, "Shift + ");

        sb.append(Character.toUpperCase(shortcut));

        return sb.toString();
    }

    private static void appendModifier(StringBuilder sb, int mask, int modifier, String label) {
        if ((mask & modifier) == modifier) {
            sb.append(label);
        }
    }

    /**
     * @return Whether this menu item should be showing shortcuts (depends on
     * whether the menu should show shortcuts and whether this item has
     * a shortcut defined)
     */
    boolean shouldShowShortcut() {
        // Show shortcuts if the menu is supposed to show shortcuts AND this item has a shortcut
        return mMenu.isShortcutsVisible() && (getShortcut() != 0);
    }

    @Override
    public SubMenu getSubMenu() {
        return mSubMenu;
    }

    @Override
    public boolean hasSubMenu() {
        return mSubMenu != null;
    }

    void setSubMenu(@Nonnull SubMenuBuilder subMenu) {
        mSubMenu = subMenu;

        subMenu.setHeaderTitle(getTitle());
    }

    @Override
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Gets the title for a particular {@link MenuView.ItemView}
     *
     * @param itemView The ItemView that is receiving the title
     * @return Either the title or condensed title based on what the ItemView
     * prefers
     */
    CharSequence getTitleForItemView(MenuView.ItemView itemView) {
        return ((itemView != null) && itemView.prefersCondensedTitle())
                ? getTitleCondensed()
                : getTitle();
    }

    @Override
    public MenuItem setTitle(CharSequence title) {
        mTitle = title;

        mMenu.onItemsChanged(false);

        if (mSubMenu != null) {
            mSubMenu.setHeaderTitle(title);
        }

        return this;
    }

    @Override
    public CharSequence getTitleCondensed() {
        return mTitleCondensed != null ? mTitleCondensed : mTitle;
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        mTitleCondensed = title;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Nullable
    @Override
    public Drawable getIcon() {
        if (mIconDrawable != null) {
            return applyIconTintIfNecessary(mIconDrawable);
        }

        return null;
    }

    @Override
    public MenuItem setIcon(Drawable icon) {
        mIconDrawable = icon;
        mNeedToApplyIconTint = true;
        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setIconTintList(@Nullable ColorStateList iconTintList) {
        mIconTintList = iconTintList;
        mHasIconTint = true;
        mNeedToApplyIconTint = true;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Nullable
    @Override
    public ColorStateList getIconTintList() {
        return mIconTintList;
    }

    private Drawable applyIconTintIfNecessary(Drawable icon) {
        if (icon != null && mNeedToApplyIconTint && (mHasIconTint)) {
            icon = icon.mutate();

            if (mHasIconTint) {
                icon.setTintList(mIconTintList);
            }

            mNeedToApplyIconTint = false;
        }

        return icon;
    }

    @Override
    public boolean isCheckable() {
        return (mFlags & CHECKABLE) == CHECKABLE;
    }

    @Override
    public MenuItem setCheckable(boolean checkable) {
        final int oldFlags = mFlags;
        mFlags = (mFlags & ~CHECKABLE) | (checkable ? CHECKABLE : 0);
        if (oldFlags != mFlags) {
            mMenu.onItemsChanged(false);
        }

        return this;
    }

    public void setExclusiveCheckable(boolean exclusive) {
        mFlags = (mFlags & ~EXCLUSIVE) | (exclusive ? EXCLUSIVE : 0);
    }

    public boolean isExclusiveCheckable() {
        return (mFlags & EXCLUSIVE) != 0;
    }

    @Override
    public boolean isChecked() {
        return (mFlags & CHECKED) == CHECKED;
    }

    @Override
    public MenuItem setChecked(boolean checked) {
        if ((mFlags & EXCLUSIVE) != 0) {
            // Call the method on the Menu since it knows about the others in this
            // exclusive checkable group
            mMenu.setExclusiveItemChecked(this);
        } else {
            setCheckedInt(checked);
        }

        return this;
    }

    void setCheckedInt(boolean checked) {
        final int oldFlags = mFlags;
        mFlags = (mFlags & ~CHECKED) | (checked ? CHECKED : 0);
        if (oldFlags != mFlags) {
            mMenu.onItemsChanged(false);
        }
    }

    @Override
    public boolean isVisible() {
        if (mActionProvider != null && mActionProvider.overridesItemVisibility()) {
            return (mFlags & HIDDEN) == 0 && mActionProvider.isVisible();
        }
        return (mFlags & HIDDEN) == 0;
    }

    /**
     * Changes the visibility of the item. This method DOES NOT notify the
     * parent menu of a change in this item, so this should only be called from
     * methods that will eventually trigger this change.  If unsure, use {@link #setVisible(boolean)}
     * instead.
     *
     * @param shown Whether to show (true) or hide (false).
     * @return Whether the item's shown state was changed
     */
    boolean setVisibleInt(boolean shown) {
        final int oldFlags = mFlags;
        mFlags = (mFlags & ~HIDDEN) | (shown ? 0 : HIDDEN);
        return oldFlags != mFlags;
    }

    @Override
    public MenuItem setVisible(boolean shown) {
        // Try to set the shown state to the given state. If the shown state was changed
        // (i.e. the previous state isn't the same as given state), notify the parent menu that
        // the shown state has changed for this item
        if (setVisibleInt(shown)) mMenu.onItemVisibleChanged(this);

        return this;
    }

    @Override
    public MenuItem setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener clickListener) {
        mClickListener = clickListener;
        return this;
    }

    @Nonnull
    @Override
    public String toString() {
        return mTitle != null ? mTitle.toString() : "";
    }

    void setMenuInfo(Object menuInfo) {
        mMenuInfo = menuInfo;
    }

    @Override
    public Object getMenuInfo() {
        return mMenuInfo;
    }

    public void actionFormatChanged() {
        mMenu.onItemActionRequestChanged(this);
    }

    /**
     * @return Whether the menu should show icons for menu items.
     */
    public boolean shouldShowIcon() {
        return mMenu.getOptionalIconsVisible();
    }

    public boolean isActionButton() {
        return (mFlags & IS_ACTION) == IS_ACTION;
    }

    public boolean requestsActionButton() {
        return (mShowAsAction & SHOW_AS_ACTION_IF_ROOM) == SHOW_AS_ACTION_IF_ROOM;
    }

    @Override
    public boolean requiresActionButton() {
        return (mShowAsAction & SHOW_AS_ACTION_ALWAYS) == SHOW_AS_ACTION_ALWAYS;
    }

    @Override
    public boolean requiresOverflow() {
        return !requiresActionButton() && !requestsActionButton();
    }

    public void setIsActionButton(boolean isActionButton) {
        if (isActionButton) {
            mFlags |= IS_ACTION;
        } else {
            mFlags &= ~IS_ACTION;
        }
    }

    public boolean showsTextAsAction() {
        return (mShowAsAction & SHOW_AS_ACTION_WITH_TEXT) == SHOW_AS_ACTION_WITH_TEXT;
    }

    @Override
    public void setShowAsAction(int actionEnum) {
        switch (actionEnum & SHOW_AS_ACTION_MASK) {
            case SHOW_AS_ACTION_ALWAYS:
            case SHOW_AS_ACTION_IF_ROOM:
            case SHOW_AS_ACTION_NEVER:
                // Looks good!
                break;

            default:
                // Mutually exclusive options selected!
                throw new IllegalArgumentException("SHOW_AS_ACTION_ALWAYS, SHOW_AS_ACTION_IF_ROOM,"
                        + " and SHOW_AS_ACTION_NEVER are mutually exclusive.");
        }
        mShowAsAction = actionEnum;
        mMenu.onItemActionRequestChanged(this);
    }

    @Override
    public MenuItem setActionView(View view) {
        mActionView = view;
        mActionProvider = null;
        if (view != null && view.getId() == View.NO_ID && mId > 0) {
            view.setId(mId);
        }
        mMenu.onItemActionRequestChanged(this);
        return this;
    }

    @Nullable
    @Override
    public View getActionView() {
        if (mActionView != null) {
            return mActionView;
        } else if (mActionProvider != null) {
            mActionView = mActionProvider.onCreateActionView(this);
            return mActionView;
        } else {
            return null;
        }
    }

    @Override
    public ActionProvider getActionProvider() {
        return mActionProvider;
    }

    @Override
    public MenuItem setActionProvider(ActionProvider actionProvider) {
        if (mActionProvider != null) {
            mActionProvider.reset();
        }
        mActionView = null;
        mActionProvider = actionProvider;
        mMenu.onItemsChanged(true); // Measurement can be changed
        if (mActionProvider != null) {
            mActionProvider.setVisibilityListener(isVisible -> mMenu.onItemVisibleChanged(MenuItemImpl.this));
        }
        return this;
    }

    @Override
    public MenuItem setShowAsActionFlags(int actionEnum) {
        setShowAsAction(actionEnum);
        return this;
    }

    @Override
    public boolean expandActionView() {
        if (!hasCollapsibleActionView()) {
            return false;
        }

        if (mOnActionExpandListener == null ||
                mOnActionExpandListener.onMenuItemActionExpand(this)) {
            return mMenu.expandItemActionView(this);
        }

        return false;
    }

    @Override
    public boolean collapseActionView() {
        if ((mShowAsAction & SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW) == 0) {
            return false;
        }
        if (mActionView == null) {
            // We're already collapsed if we have no action view.
            return true;
        }

        if (mOnActionExpandListener == null ||
                mOnActionExpandListener.onMenuItemActionCollapse(this)) {
            return mMenu.collapseItemActionView(this);
        }

        return false;
    }

    @Override
    public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
        mOnActionExpandListener = listener;
        return this;
    }

    public boolean hasCollapsibleActionView() {
        if ((mShowAsAction & SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW) != 0) {
            if (mActionView == null && mActionProvider != null) {
                mActionView = mActionProvider.onCreateActionView(this);
            }
            return mActionView != null;
        }
        return false;
    }

    public void setActionViewExpanded(boolean isExpanded) {
        mIsActionViewExpanded = isExpanded;
        mMenu.onItemsChanged(false);
    }

    @Override
    public boolean isActionViewExpanded() {
        return mIsActionViewExpanded;
    }

    @Override
    public MenuItem setContentDescription(CharSequence contentDescription) {
        mContentDescription = contentDescription;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public CharSequence getContentDescription() {
        return mContentDescription;
    }

    @Override
    public MenuItem setTooltipText(CharSequence tooltipText) {
        mTooltipText = tooltipText;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public CharSequence getTooltipText() {
        return mTooltipText;
    }
}
