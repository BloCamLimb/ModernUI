/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import javax.annotation.Nullable;

/**
 * Defines an object that can work as a parent of a View.
 */
public interface ViewParent {

    /**
     * Returns the parent of this ViewParent.
     *
     * @return the parent or {@code null} if the parent is not available
     */
    @Nullable
    ViewParent getParent();

    /**
     * Called when something has changed which has invalidated the layout of a
     * child of this view parent. This will schedule a layout pass of the view tree.
     */
    void requestLayout();

    /**
     * Tells if this view parent can resolve the layout direction.
     * See {@link View#setLayoutDirection(int)}
     *
     * @return True if this view parent can resolve the layout direction.
     */
    boolean canResolveLayoutDirection();

    /**
     * Tells if this view parent layout direction is resolved.
     * See {@link View#setLayoutDirection(int)}
     *
     * @return True if this view parent layout direction is resolved.
     */
    boolean isLayoutDirectionResolved();

    /**
     * Return this view parent layout direction. See {@link View#getLayoutDirection()}
     *
     * @return {@link View#LAYOUT_DIRECTION_RTL} if the layout direction is RTL or returns
     * {@link View#LAYOUT_DIRECTION_LTR} if the layout direction is not RTL.
     */
    int getLayoutDirection();

    /**
     * Tells if this view parent can resolve the text direction.
     * See {@link View#setTextDirection(int)}
     *
     * @return True if this view parent can resolve the text direction.
     */
    boolean canResolveTextDirection();

    /**
     * Tells if this view parent text direction is resolved.
     * See {@link View#setTextDirection(int)}
     *
     * @return True if this view parent text direction is resolved.
     */
    boolean isTextDirectionResolved();

    /**
     * Return this view parent text direction. See {@link View#getTextDirection()}
     *
     * @return the resolved text direction. Returns one of:
     *
     * {@link View#TEXT_DIRECTION_FIRST_STRONG}
     * {@link View#TEXT_DIRECTION_ANY_RTL},
     * {@link View#TEXT_DIRECTION_LTR},
     * {@link View#TEXT_DIRECTION_RTL},
     * {@link View#TEXT_DIRECTION_LOCALE}
     */
    int getTextDirection();

    /**
     * Tells if this view parent can resolve the text alignment.
     * See {@link View#setTextAlignment(int)}
     *
     * @return True if this view parent can resolve the text alignment.
     */
    boolean canResolveTextAlignment();

    /**
     * Tells if this view parent text alignment is resolved.
     * See {@link View#setTextAlignment(int)}
     *
     * @return True if this view parent text alignment is resolved.
     */
    boolean isTextAlignmentResolved();

    /**
     * Return this view parent text alignment. See {@link View#getTextAlignment()}
     *
     * @return the resolved text alignment. Returns one of:
     *
     * {@link View#TEXT_ALIGNMENT_GRAVITY},
     * {@link View#TEXT_ALIGNMENT_CENTER},
     * {@link View#TEXT_ALIGNMENT_TEXT_START},
     * {@link View#TEXT_ALIGNMENT_TEXT_END},
     * {@link View#TEXT_ALIGNMENT_VIEW_START},
     * {@link View#TEXT_ALIGNMENT_VIEW_END}
     */
    int getTextAlignment();

    /**
     * This method is called on the parent when a child's drawable state
     * has changed.
     *
     * @param child The child whose drawable state has changed.
     */
    void childDrawableStateChanged(View child);
}
