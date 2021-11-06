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

package icyllis.modernui.text.method;

import icyllis.modernui.text.Spannable;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.widget.TextView;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides cursor positioning, scrolling and text selection functionality in a {@link TextView}.
 * <p>
 * The {@link TextView} delegates handling of key events, trackball motions and touches to
 * the movement method for purposes of content navigation.  The framework automatically
 * selects an appropriate movement method based on the content of the {@link TextView}.
 * </p><p>
 * This interface is intended for use by the framework; it should not be implemented
 * directly by applications.
 * </p>
 */
@ParametersAreNonnullByDefault
public interface MovementMethod {

    void initialize(TextView widget, Spannable text);

    boolean onKeyDown(TextView widget, Spannable text, int keyCode, KeyEvent event);

    boolean onKeyUp(TextView widget, Spannable text, int keyCode, KeyEvent event);

    /**
     * If the key listener wants to other kinds of key events, return true,
     * otherwise return false and the caller (i.e. the widget host)
     * will handle the key.
     */
    boolean onKeyOther(TextView widget, Spannable text, KeyEvent event);

    void onTakeFocus(TextView widget, Spannable text, int direction);

    boolean onTouchEvent(TextView widget, Spannable text, MotionEvent event);

    boolean onGenericMotionEvent(TextView widget, Spannable text, MotionEvent event);

    /**
     * Returns true if this movement method allows arbitrary selection
     * of any text; false if it has no selection (like a movement method
     * that only scrolls) or a constrained selection (for example
     * limited to links.  The "Select All" menu item is disabled
     * if arbitrary selection is not allowed.
     */
    boolean canSelectArbitrarily();
}
