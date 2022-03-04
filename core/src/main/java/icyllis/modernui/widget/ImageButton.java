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

import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.PointerIcon;

import javax.annotation.Nonnull;

/**
 * <p>
 * Displays a button with an image (instead of text) that can be pressed
 * or clicked by the user. By default, an ImageButton looks like a regular
 * {@link Button}, with the standard button background
 * that changes color during different button states.
 *
 * <p>To remove the standard button background image, define your own
 * background image or set the background color to be transparent.</p>
 *
 * <p>See the <a href="https://developer.android.com/guide/topics/ui/controls/button">Buttons</a>
 * guide.</p>
 */
public class ImageButton extends ImageView {

    public ImageButton() {
        setFocusable(true);
        setClickable(true);
    }

    @Override
    public PointerIcon onResolvePointerIcon(@Nonnull MotionEvent event) {
        if (isClickable() && isEnabled()) {
            return PointerIcon.getSystemIcon(PointerIcon.TYPE_HAND);
        }
        return super.onResolvePointerIcon(event);
    }
}
