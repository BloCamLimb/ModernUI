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

package icyllis.modernui.widget;

import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.resources.ResourceId;
import icyllis.modernui.util.AttributeSet;

/**
 * <p>
 * Displays a button with an image (instead of text) that can be pressed
 * or clicked by the user. The image on the surface of the button is defined
 * by the {@link #setImage(Image)} method.
 *
 * <p>To indicate the different button states (focused, selected, etc.), you can
 * define a different image for each state. E.g., a blue image by default, an
 * orange one for when focused, and a yellow one for when pressed.
 * An easy way to do this is with {@link StateListDrawable} and
 * {@link #setImageDrawable(Drawable)}. For example:</p>
 *
 * <pre>{@code
 * StateListDrawable selector = new StateListDrawable();
 * selector.addState(new int[]{-R.attr.state_enabled}, disabledIcon);
 * selector.addState(new int[]{R.attr.state_checkable, R.attr.state_checked}, checkedIcon);
 * selector.addState(new int[]{R.attr.state_checkable}, uncheckedIcon);
 * selector.addState(StateSet.WILD_CARD, uncheckableIcon);
 * imageButton.setImageDrawable(selector);
 * }</pre>
 *
 * <p>See {@link CheckableImageButton} for checkable image buttons.</p>
 *
 * <p>See the <a href="https://developer.android.com/guide/topics/ui/controls/button">Buttons</a>
 * guide.</p>
 */
public class ImageButton extends ImageView {

    public ImageButton(Context context) {
        super(context);
        setFocusable(true);
        setClickable(true);
    }

    public ImageButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, null);
    }

    public ImageButton(Context context, @Nullable AttributeSet attrs,
                       @Nullable @AttrRes ResourceId defStyleAttr) {
        this(context, attrs, defStyleAttr, null);
    }

    public ImageButton(Context context, @Nullable AttributeSet attrs,
                       @Nullable @AttrRes ResourceId defStyleAttr,
                       @Nullable @StyleRes ResourceId defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setFocusable(true);
    }
}
