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

import icyllis.modernui.fragment.Fragment;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * An ActionProvider defines rich menu interaction in a single component.
 * ActionProvider can generate action views for use in the action bar,
 * dynamically populate submenus of a MenuItem, and handle default menu
 * item invocations.
 *
 * <p>An ActionProvider can be optionally specified for a {@link MenuItem} and will be
 * responsible for creating the action view that appears in the ActionBar
 * in place of a simple button in the bar. When the menu item is presented in a way that
 * does not allow custom action views, (e.g. in an overflow menu,) the ActionProvider
 * can perform a default action.</p>
 *
 * <p>There are two ways to use an action provider:
 * <ul>
 * <li>
 * Set the action provider on a {@link MenuItem} directly by calling
 * {@link MenuItem#setActionProvider(ActionProvider)}.
 * </li>
 * </ul>
 *
 * @see MenuItem#setActionProvider(ActionProvider)
 * @see MenuItem#getActionProvider()
 */
public abstract class ActionProvider {

    private static final Marker MARKER = MarkerManager.getMarker("ActionProvider");

    private SubUiVisibilityListener mSubUiVisibilityListener;
    private VisibilityListener mVisibilityListener;

    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     */
    public ActionProvider() {
    }

    /**
     * Factory method called by the Android framework to create new action views.
     * This method returns a new action view for the given MenuItem.
     *
     * @param menuItem MenuItem to create the action view for
     * @return the new action view
     */
    public abstract View onCreateActionView(MenuItem menuItem);

    /**
     * The result of this method determines whether or not {@link #isVisible()} will be used
     * by the {@link MenuItem} this ActionProvider is bound to help determine its visibility.
     *
     * @return true if this ActionProvider overrides the visibility of the MenuItem
     * it is bound to, false otherwise. The default implementation returns false.
     * @see #isVisible()
     */
    public boolean overridesItemVisibility() {
        return false;
    }

    /**
     * If {@link #overridesItemVisibility()} returns true, the return value of this method
     * will help determine the visibility of the {@link MenuItem} this ActionProvider is bound to.
     *
     * <p>If the MenuItem's visibility is explicitly set to false by the application,
     * the MenuItem will not be shown, even if this method returns true.</p>
     *
     * @return true if the MenuItem this ActionProvider is bound to is visible, false if
     * it is invisible. The default implementation returns true.
     */
    public boolean isVisible() {
        return true;
    }

    /**
     * If this ActionProvider is associated with an item in a menu,
     * refresh the visibility of the item based on {@link #overridesItemVisibility()} and
     * {@link #isVisible()}. If {@link #overridesItemVisibility()} returns false, this call
     * will have no effect.
     */
    public void refreshVisibility() {
        if (mVisibilityListener != null && overridesItemVisibility()) {
            mVisibilityListener.onActionProviderVisibilityChanged(isVisible());
        }
    }

    /**
     * Performs an optional default action.
     * <p>
     * For the case of an action provider placed in a menu item not shown as an action this
     * method is invoked if previous callbacks for processing menu selection has handled
     * the event.
     * </p>
     * <p>
     * A menu item selection is processed in the following order:
     * <ul>
     * <li>
     * Receiving a call to {@link MenuItem.OnMenuItemClickListener#onMenuItemClick
     *  MenuItem.OnMenuItemClickListener.onMenuItemClick}.
     * </li>
     * <li>
     * Receiving a call to {@link Fragment#onOptionsItemSelected(MenuItem)
     *  Fragment.onOptionsItemSelected(MenuItem)}
     * </li>
     * <li>
     * Invoking this method.
     * </li>
     * </ul>
     * </p>
     * <p>
     * The default implementation does not perform any action and returns false.
     * </p>
     */
    public boolean onPerformDefaultAction() {
        return false;
    }

    /**
     * Determines if this ActionProvider has a submenu associated with it.
     *
     * <p>Associated submenus will be shown when an action view is not. This
     * provider instance will receive a call to {@link #onPrepareSubMenu(SubMenu)}
     * after the call to {@link #onPerformDefaultAction()} and before a submenu is
     * displayed to the user.
     *
     * @return true if the item backed by this provider should have an associated submenu
     */
    public boolean hasSubMenu() {
        return false;
    }

    /**
     * Called to prepare an associated submenu for the menu item backed by this ActionProvider.
     *
     * <p>if {@link #hasSubMenu()} returns true, this method will be called when the
     * menu item is selected to prepare the submenu for presentation to the user. Apps
     * may use this to create or alter submenu content right before display.
     *
     * @param subMenu Submenu that will be displayed
     */
    public void onPrepareSubMenu(SubMenu subMenu) {
    }

    /**
     * Notify the system that the visibility of an action view's sub-UI such as
     * an anchored popup has changed. This will affect how other system
     * visibility notifications occur.
     *
     * @hide Pending future API approval
     */
    public void subUiVisibilityChanged(boolean isVisible) {
        if (mSubUiVisibilityListener != null) {
            mSubUiVisibilityListener.onSubUiVisibilityChanged(isVisible);
        }
    }

    @ApiStatus.Internal
    public void setSubUiVisibilityListener(SubUiVisibilityListener listener) {
        mSubUiVisibilityListener = listener;
    }

    /**
     * Set a listener to be notified when this ActionProvider's overridden visibility changes.
     * This should only be used by MenuItem implementations.
     *
     * @param listener listener to set
     */
    public void setVisibilityListener(VisibilityListener listener) {
        if (mVisibilityListener != null) {
            LOGGER.warn(MARKER, "setVisibilityListener: Setting a new ActionProvider.VisibilityListener " +
                    "when one is already set. Are you reusing this {} instance while it is still in use " +
                    "somewhere else?", getClass().getSimpleName());
        }
        mVisibilityListener = listener;
    }

    @ApiStatus.Internal
    public void reset() {
        mVisibilityListener = null;
        mSubUiVisibilityListener = null;
    }

    @ApiStatus.Internal
    @FunctionalInterface
    public interface SubUiVisibilityListener {

        void onSubUiVisibilityChanged(boolean isVisible);
    }

    /**
     * Listens to changes in visibility as reported by {@link ActionProvider#refreshVisibility()}.
     *
     * @see ActionProvider#overridesItemVisibility()
     * @see ActionProvider#isVisible()
     */
    @FunctionalInterface
    public interface VisibilityListener {

        void onActionProviderVisibilityChanged(boolean isVisible);
    }
}
