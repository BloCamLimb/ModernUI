/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.ui.layout;

import icyllis.modernui.view.ViewGroup;

//TODO developing
public class GridLayout extends ViewGroup {

    /**
     * The constant used to indicate that a value is undefined.
     * Fields can use this value to indicate that their values
     * have not yet been set. Similarly, methods can return this value
     * to indicate that there is no suitable value that the implementation
     * can return.
     * The value used for the constant (currently {@link Integer#MIN_VALUE}) is
     * intended to avoid confusion between valid values whose sign may not be known.
     */
    public static final int UNDEFINED = Integer.MIN_VALUE;


    /**
     * @see #setOrientation(Orientation)
     */
    private Orientation orientation = Orientation.HORIZONTAL;

    @Override
    protected void onLayout(boolean changed) {

    }

    /**
     * GridLayout uses the orientation property for two purposes:
     * <ul>
     *  <li>
     *      To control the 'direction' in which default row/column indices are generated
     *      when they are not specified in a component's layout parameters.
     *  </li>
     *  <li>
     *      To control which axis should be processed first during the layout operation:
     *      when orientation is {@link Orientation#HORIZONTAL} the horizontal axis is laid out first.
     *  </li>
     * </ul>
     * <p>
     * The order in which axes are laid out is important if, for example, the height of
     * one of GridLayout's children is dependent on its width - and its width is, in turn,
     * dependent on the widths of other components.
     * <p>
     * If your layout contains a TextView (or derivative:
     * {@code Button}, {@code EditText}, {@code CheckBox}, etc.) which is
     * in multi-line mode (the default) it is normally best to leave GridLayout's
     * orientation as {@code HORIZONTAL} - because {@code TextView} is capable of
     * deriving its height for a given width, but not the other way around.
     * <p>
     * Other than the effects above, orientation does not affect the actual layout operation of
     * GridLayout, so it's fine to leave GridLayout in {@code HORIZONTAL} mode even if
     * the height of the intended layout greatly exceeds its width.
     * <p>
     * The default value of this property is {@link Orientation#HORIZONTAL}.
     *
     * @param orientation either {@link Orientation#HORIZONTAL} or {@link Orientation#VERTICAL}
     * @see #getOrientation()
     */
    public void setOrientation(Orientation orientation) {
        if (this.orientation != orientation) {
            this.orientation = orientation;
            requestLayout();
        }
    }

    /**
     * Returns the current orientation.
     *
     * @return either {@link Orientation#HORIZONTAL} or {@link Orientation#VERTICAL}
     * @see #setOrientation(Orientation)
     */
    public Orientation getOrientation() {
        return orientation;
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }
}
